package com.foodfusion.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsSummary(long orderCount, BigDecimal revenue) {
}
