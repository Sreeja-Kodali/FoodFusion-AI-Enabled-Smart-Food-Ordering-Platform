package com.foodfusion.orderservice.service;

import com.foodfusion.orderservice.dto.InventoryResponse;
import com.foodfusion.orderservice.dto.OrderLineItemsDTO;
import com.foodfusion.orderservice.dto.OrderRequest;
import com.foodfusion.orderservice.model.Order;
import com.foodfusion.orderservice.model.OrderLineItem;
import com.foodfusion.orderservice.producer.OrderProducer;
import com.foodfusion.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.foodfusion.orderservice.constants.Constants.RESPONSE_201;
import static com.foodfusion.orderservice.constants.Constants.RESPONSE_400;
import static com.foodfusion.orderservice.constants.Constants.RESPONSE_404;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;
    private final WebClient.Builder webClientBuilder;
    private final OrderProducer orderProducer;

    public ResponseEntity<Object> placeOrder(OrderRequest orderRequest) {
        return placeOrder(orderRequest, "anonymous");
    }

    public ResponseEntity<Object> placeOrder(OrderRequest orderRequest, String userId) {
        if (orderRequest.getOrderLineItemsDTOs() == null || orderRequest.getOrderLineItemsDTOs().isEmpty()) {
            return returnResponse("Order must contain at least one item.", HttpStatus.BAD_REQUEST);
        }

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setUserId(userId == null || userId.isBlank() ? "anonymous" : userId);
        order.setStatus("CONFIRMED");
        order.setCreatedAt(Instant.now());
        order.setOrderLineItems(orderRequest.getOrderLineItemsDTOs().stream()
                .map(this::mapToDto)
                .toList());

        List<String> skuCodes = orderRequest.getOrderLineItemsDTOs().stream()
                .map(OrderLineItemsDTO::getSkuCode)
                .toList();
        InventoryResponse[] inventory = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        if (inventory == null || inventory.length == 0) {
            return returnResponse(RESPONSE_400, HttpStatus.BAD_REQUEST);
        }

        boolean inStock = Arrays.stream(inventory).allMatch(InventoryResponse::isInStock);
        if (!inStock) {
            return returnResponse(RESPONSE_404, HttpStatus.NOT_FOUND);
        }

        Order savedOrder = orderRepository.save(order);
        orderProducer.sendMessage(savedOrder);
        return returnResponse(RESPONSE_201, HttpStatus.CREATED);
    }

    public List<Order> getOrderHistory(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId == null || userId.isBlank() ? "anonymous" : userId);
    }

    public Optional<Order> getOrder(String orderNumber) {
        return Optional.ofNullable(orderRepository.findByOrderNumber(orderNumber));
    }

    private OrderLineItem mapToDto(OrderLineItemsDTO orderLineItemsDto) {
        return OrderLineItem.builder()
                .skuCode(orderLineItemsDto.getSkuCode())
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .build();
    }

    private ResponseEntity<Object> returnResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new HashMap<String, String>() {{
            put("Message", message);
            put("Status", status.toString());
        }}, status);
    }
}
