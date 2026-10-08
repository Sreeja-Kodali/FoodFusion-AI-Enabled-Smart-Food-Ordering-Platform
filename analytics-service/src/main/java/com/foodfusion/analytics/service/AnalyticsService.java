package com.foodfusion.analytics.service;

import com.foodfusion.analytics.dto.AnalyticsSummary;
import com.foodfusion.analytics.dto.DailyAnalytics;
import com.foodfusion.analytics.model.OrderAnalyticsRecord;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class AnalyticsService {
    private final MongoTemplate mongoTemplate;

    public AnalyticsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public AnalyticsSummary summary() {
        Aggregation pipeline = newAggregation(
                match(Criteria.where("status").is("CONFIRMED")),
                group().count().as("orderCount").sum("totalAmount").as("revenue")
        );
        AggregationResults<AnalyticsSummary> result = mongoTemplate.aggregate(
                pipeline, "order_analytics", AnalyticsSummary.class);
        AnalyticsSummary summary = result.getUniqueMappedResult();
        return summary == null
                ? new AnalyticsSummary(0, BigDecimal.ZERO)
                : summary;
    }

    public List<DailyAnalytics> daily() {
        Aggregation pipeline = newAggregation(
                match(Criteria.where("status").is("CONFIRMED")),
                project()
                        .andExpression("dateToString('%Y-%m-%d', occurredAt)").as("date")
                        .and("totalAmount").as("totalAmount"),
                group("date")
                        .count().as("orderCount")
                        .sum("totalAmount").as("revenue"),
                project("orderCount", "revenue").and("_id").as("date"),
                sort(Sort.Direction.ASC, "date")
        );
        AggregationResults<DailyAnalytics> results = mongoTemplate.aggregate(
                pipeline, OrderAnalyticsRecord.class, DailyAnalytics.class);
        return results.getMappedResults();
    }
}
