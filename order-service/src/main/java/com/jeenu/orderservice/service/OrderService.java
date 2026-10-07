package com.jeenu.orderservice.service;

import com.jeenu.orderservice.dto.InventoryResponse;
import com.jeenu.orderservice.dto.OrderLineItemsDTO;
import com.jeenu.orderservice.dto.OrderRequest;
import com.jeenu.orderservice.model.Order;
import com.jeenu.orderservice.model.OrderLineItem;
import com.jeenu.orderservice.producer.OrderProducer;
import com.jeenu.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static com.jeenu.orderservice.constants.Constants.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;
    private final WebClient.Builder webClientBuilder;
    private final OrderProducer orderProducer;


    public ResponseEntity<Object> placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItem> orderLineItems = orderRequest.getOrderLineItemsDTOs().stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItems(orderLineItems);

        // get all the list of skuCodes ordered to pass to Inventory service
        List<String> listOfSkuCodes = orderRequest.getOrderLineItemsDTOs().stream()
                .map(OrderLineItemsDTO::getSkuCode)
                .toList();

        /**
         *  call the Inventory service endpoint and place the order is the stock is
         *  available for each every product placed
         */
        InventoryResponse[] arrayOfInventoryResponse = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode",listOfSkuCodes)
                                .build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)  //convert List of InventoryResponse to Array of InventoryResponse
                .block(); // synchronous call
        if(arrayOfInventoryResponse.length == 0)
            return returnResponse(RESPONSE_400, HttpStatus.BAD_REQUEST);

        Boolean result =  Arrays.stream(arrayOfInventoryResponse)
                .allMatch(InventoryResponse::isInStock);

        if(Boolean.TRUE.equals(result)){
            orderRepository.save(order);
            Order orderEvent =  orderRepository.findByOrderNumber(order.getOrderNumber());
            orderProducer.sendMessage(orderEvent);

            return returnResponse(RESPONSE_201, HttpStatus.CREATED);
        }
        else
            return returnResponse(RESPONSE_404, HttpStatus.NOT_FOUND);
    }

    private OrderLineItem mapToDto(OrderLineItemsDTO orderLineItemsDto) {
        return OrderLineItem.builder()
                .skuCode(orderLineItemsDto.getSkuCode())
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .build();
    }
    private ResponseEntity<Object> returnResponse(String message, HttpStatus status){
        return new ResponseEntity<>(new HashMap<String, String>() {{
            put("Message", message);
            put("Status", status.toString());
        }}, status);
    }

}
