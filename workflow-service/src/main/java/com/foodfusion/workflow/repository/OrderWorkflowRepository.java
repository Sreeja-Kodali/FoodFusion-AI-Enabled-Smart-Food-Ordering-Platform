package com.foodfusion.workflow.repository;

import com.foodfusion.workflow.model.OrderWorkflow;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrderWorkflowRepository extends MongoRepository<OrderWorkflow, String> {
    Optional<OrderWorkflow> findByOrderNumber(String orderNumber);
}
