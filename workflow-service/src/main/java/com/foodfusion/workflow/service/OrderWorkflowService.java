package com.foodfusion.workflow.service;

import com.foodfusion.avro.generated.OrderEvent;
import com.foodfusion.avro.generated.OrderLineItemsEvent;
import com.foodfusion.events.OrderStatusEvent;
import com.foodfusion.workflow.model.OrderWorkflow;
import com.foodfusion.workflow.repository.OrderWorkflowRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class OrderWorkflowService {
    private final OrderWorkflowRepository workflows;
    private final KafkaTemplate<String, OrderStatusEvent> statusEvents;

    public OrderWorkflowService(
            OrderWorkflowRepository workflows,
            KafkaTemplate<String, OrderStatusEvent> statusEvents
    ) {
        this.workflows = workflows;
        this.statusEvents = statusEvents;
    }

    @KafkaListener(topics = "${foodfusion.kafka.order-topic:order-topic}")
    public void processOrder(OrderEvent orderEvent) throws Exception {
        Optional<OrderWorkflow> existing = workflows.findByOrderNumber(orderEvent.getOrderNumber().toString());
        if (existing.isPresent() && "CONFIRMED".equals(existing.get().getStage())) {
            return;
        }

        BigDecimal total = orderEvent.getOrderLineItemsList().stream()
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String userId = orderEvent.getUserId() == null
                ? "anonymous"
                : orderEvent.getUserId().toString();
        Instant occurredAt = Instant.now();

        OrderWorkflow workflow = existing.orElseGet(OrderWorkflow::new);
        workflow.setOrderNumber(orderEvent.getOrderNumber().toString());
        workflow.setUserId(userId);
        workflow.setStage("CONFIRMED");
        workflow.setTotalAmount(total);
        workflow.setUpdatedAt(occurredAt);
        workflows.save(workflow);

        OrderStatusEvent statusEvent = new OrderStatusEvent(
                workflow.getOrderNumber(), userId, workflow.getStage(), total, occurredAt);
        statusEvents.send("order-status-topic", workflow.getOrderNumber(), statusEvent).get();
    }

    private BigDecimal lineTotal(OrderLineItemsEvent item) {
        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
