package com.foodfusion.inventoryservice;


import com.foodfusion.inventoryservice.model.Inventory;
import com.foodfusion.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class,args);
    }
    @Bean
    public CommandLineRunner loadData(InventoryRepository inventoryRepository){
        return args -> {
            Inventory inventory1Data = new Inventory(1L,"iphone_13",20);
            Inventory inventory2Data = new Inventory(2L,"iphone_12",0);

            inventoryRepository.save(inventory1Data);
            inventoryRepository.save(inventory2Data);
        };
    }
}