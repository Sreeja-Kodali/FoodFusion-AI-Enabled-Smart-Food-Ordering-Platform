package com.foodfusion.inventoryservice.config;

import com.foodfusion.inventoryservice.model.Inventory;
import com.foodfusion.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class InventoryBootstrap {
    @Bean
    @ConditionalOnProperty(name = "foodfusion.seed-data.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedInventory(InventoryRepository inventory) {
        return args -> {
            if (inventory.count() == 0) {
                inventory.saveAll(List.of(
                        new Inventory(null, "classic-margherita", 24),
                        new Inventory(null, "garden-bowl", 18),
                        new Inventory(null, "crispy-chicken-burger", 12),
                        new Inventory(null, "mushroom-pasta", 10),
                        new Inventory(null, "berry-cheesecake", 8)
                ));
            }
        };
    }
}
