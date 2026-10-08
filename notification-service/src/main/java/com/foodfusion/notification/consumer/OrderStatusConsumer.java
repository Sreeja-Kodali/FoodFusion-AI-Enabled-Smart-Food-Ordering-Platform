package com.foodfusion.notification.consumer;

import com.foodfusion.events.OrderStatusEvent;
import com.foodfusion.notification.model.OrderNotification;
import com.foodfusion.notification.repository.OrderNotificationRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusConsumer {
    private final OrderNotificationRepository notifications;

    public OrderStatusConsumer(OrderNotificationRepository notifications) {
        this.notifications = notifications;
    }

    @KafkaListener(topics = "${foodfusion.kafka.order-status-topic:order-status-topic}")
    public void onOrderStatus(OrderStatusEvent event) {
        if (notifications.existsByOrderNumberAndStatus(event.orderNumber(), event.status())) {
            return;
        }

        OrderNotification notification = new OrderNotification();
        notification.setOrderNumber(event.orderNumber());
        notification.setUserId(event.userId());
        notification.setStatus(event.status());
        notification.setTotalAmount(event.totalAmount());
        notification.setCreatedAt(event.occurredAt());
        notification.setMessage("Order " + event.orderNumber() + " is " + event.status().toLowerCase() + ".");
        try {
            notifications.save(notification);
        } catch (DuplicateKeyException duplicateDelivery) {
            // Kafka is at-least-once; the compound key makes redelivery idempotent.
        }
    }
}
