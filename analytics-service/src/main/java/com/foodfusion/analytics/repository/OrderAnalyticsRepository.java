package com.foodfusion.analytics.repository;

import com.foodfusion.analytics.model.OrderAnalyticsRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderAnalyticsRepository extends MongoRepository<OrderAnalyticsRecord, String> {
    boolean existsByOrderNumberAndStatus(String orderNumber, String status);
}
