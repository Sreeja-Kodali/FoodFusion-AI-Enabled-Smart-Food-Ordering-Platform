package com.foodfusion.orderservice.producer;

import com.foodfusion.avro.generated.OrderEvent;
import com.foodfusion.avro.generated.OrderLineItemsEvent;
import com.foodfusion.orderservice.model.Order;
import com.foodfusion.orderservice.model.OrderLineItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class OrderProducer {
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void sendMessage(Order order){

        OrderEvent orderEvent = maptoOrderEvent(order);
        log.info("orderEvent - {} ",orderEvent.toString());
        try {
            var producerRecord = new ProducerRecord<>("order-topic", orderEvent.getOrderNumber(), orderEvent);
            CompletableFuture<SendResult<String, OrderEvent>> completableFuture = kafkaTemplate.send(producerRecord);

            // Using get() to retrieve the result (blocks until completion)
            SendResult<String, OrderEvent> sendResult = completableFuture.get();
            log.info("Order Produced Successfully to the - {}  - with key : {}", sendResult.getRecordMetadata().topic(),orderEvent.getOrderNumber());  // Access topic name from SendResult


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing order event.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not publish order event to Kafka.", e);
        }
    }

    private OrderEvent maptoOrderEvent(Order order) {
        return OrderEvent.newBuilder()
                .setId(order.getId())
                .setOrderNumber(order.getOrderNumber())
                .setUserId(order.getUserId() == null ? "anonymous" : order.getUserId())
                .setOrderLineItemsList(GetOrderLineItemsList(order.getOrderLineItems()))
                .build();
    }
    private List<OrderLineItemsEvent> GetOrderLineItemsList(List<OrderLineItem> orderLineItemList){
        return orderLineItemList.stream()
                .map(this::mapToOrderLineItemsEvent)
                .toList();
    }
    private OrderLineItemsEvent mapToOrderLineItemsEvent(OrderLineItem orderLineItem){
        return OrderLineItemsEvent.newBuilder()
                .setId(orderLineItem.getId())
                .setSkuCode(orderLineItem.getSkuCode())
                .setPrice(orderLineItem.getPrice())
                .setQuantity(orderLineItem.getQuantity())
                .build();
    }
}
