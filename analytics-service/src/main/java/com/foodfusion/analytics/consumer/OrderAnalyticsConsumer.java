package com.foodfusion.analytics.consumer;

import com.foodfusion.analytics.model.OrderAnalyticsRecord;
import com.foodfusion.analytics.repository.OrderAnalyticsRepository;
import com.foodfusion.events.OrderStatusEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderAnalyticsConsumer {
    private final OrderAnalyticsRepository analytics;

    public OrderAnalyticsConsumer(OrderAnalyticsRepository analytics) {
        this.analytics = analytics;
    }

    @KafkaListener(topics = "${foodfusion.kafka.order-status-topic:order-status-topic}")
    public void onOrderStatus(OrderStatusEvent event) {
        if (analytics.existsByOrderNumberAndStatus(event.orderNumber(), event.status())) {
            return;
        }
        OrderAnalyticsRecord record = new OrderAnalyticsRecord();
        record.setOrderNumber(event.orderNumber());
        record.setUserId(event.userId());
        record.setStatus(event.status());
        record.setTotalAmount(event.totalAmount());
        record.setOccurredAt(event.occurredAt());
        try {
            analytics.save(record);
        } catch (DuplicateKeyException duplicateDelivery) {
            // Kafka redelivery is idempotent by order number and status.
        }
    }
}
