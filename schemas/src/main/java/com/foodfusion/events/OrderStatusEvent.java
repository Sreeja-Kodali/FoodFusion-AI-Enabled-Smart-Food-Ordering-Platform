package com.foodfusion.events;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderStatusEvent(
        String orderNumber,
        String userId,
        String status,
        BigDecimal totalAmount,
        Instant occurredAt
) {
}
