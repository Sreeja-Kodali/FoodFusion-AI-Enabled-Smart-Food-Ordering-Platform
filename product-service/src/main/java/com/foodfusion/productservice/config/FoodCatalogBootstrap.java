package com.foodfusion.productservice.config;

import com.foodfusion.productservice.model.Product;
import com.foodfusion.productservice.repository.ProductRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class FoodCatalogBootstrap {
    @Bean
    @ConditionalOnProperty(name = "foodfusion.seed-data.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedFoodCatalog(ProductRepository products) {
        return args -> {
            if (products.count() == 0) {
                products.saveAll(List.of(
                        food("classic-margherita", "Classic Margherita", "Tomato, mozzarella, basil", "Pizza", "12.50"),
                        food("garden-bowl", "Garden Bowl", "Seasonal vegetables, grains, lemon dressing", "Healthy", "10.00"),
                        food("crispy-chicken-burger", "Crispy Chicken Burger", "Crispy chicken, lettuce, house sauce", "Burgers", "13.25"),
                        food("mushroom-pasta", "Mushroom Pasta", "Creamy mushroom sauce and parmesan", "Pasta", "14.00"),
                        food("berry-cheesecake", "Berry Cheesecake", "Vanilla cheesecake with seasonal berries", "Dessert", "7.50")
                ));
            }
        };
    }

    private Product food(String skuCode, String name, String description, String category, String price) {
        return Product.builder()
                .name(name)
                .skuCode(skuCode)
                .description(description)
                .category(category)
                .price(new BigDecimal(price))
                .available(true)
                .build();
    }
}
