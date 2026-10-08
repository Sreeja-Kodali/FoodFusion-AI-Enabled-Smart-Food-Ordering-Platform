package com.foodfusion.notification.repository;

import com.foodfusion.notification.model.OrderNotification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderNotificationRepository extends MongoRepository<OrderNotification, String> {
    List<OrderNotification> findByUserIdOrderByCreatedAtDesc(String userId);
    List<OrderNotification> findByOrderNumberOrderByCreatedAtDesc(String orderNumber);
    boolean existsByOrderNumberAndStatus(String orderNumber, String status);
}
