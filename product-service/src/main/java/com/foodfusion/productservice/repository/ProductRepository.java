package com.foodfusion.productservice.repository;

import com.foodfusion.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
    java.util.List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description);

    java.util.List<Product> findByCategoryIgnoreCase(String category);
}