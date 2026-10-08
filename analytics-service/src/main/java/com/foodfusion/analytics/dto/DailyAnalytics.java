package com.foodfusion.analytics.dto;

import java.math.BigDecimal;

public record DailyAnalytics(String date, long orderCount, BigDecimal revenue) {
}
