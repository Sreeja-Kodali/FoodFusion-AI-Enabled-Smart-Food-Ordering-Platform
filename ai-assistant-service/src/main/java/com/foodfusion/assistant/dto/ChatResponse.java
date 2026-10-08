package com.foodfusion.assistant.dto;

import java.util.List;

public record ChatResponse(String reply, String mode, List<FoodItem> foods) {
}
