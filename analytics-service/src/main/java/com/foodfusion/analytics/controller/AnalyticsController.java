package com.foodfusion.analytics.controller;

import com.foodfusion.analytics.dto.AnalyticsSummary;
import com.foodfusion.analytics.dto.DailyAnalytics;
import com.foodfusion.analytics.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummary summary() {
        return analyticsService.summary();
    }

    @GetMapping("/daily")
    public List<DailyAnalytics> daily() {
        return analyticsService.daily();
    }
}
