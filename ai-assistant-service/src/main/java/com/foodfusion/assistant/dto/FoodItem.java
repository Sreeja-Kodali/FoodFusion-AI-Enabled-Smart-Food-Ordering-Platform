package com.foodfusion.assistant.dto;

import java.math.BigDecimal;

public record FoodItem(
        String id,
        String name,
        String skuCode,
        String description,
        BigDecimal price,
        String category,
        String imageUrl,
        boolean available
) {
}
