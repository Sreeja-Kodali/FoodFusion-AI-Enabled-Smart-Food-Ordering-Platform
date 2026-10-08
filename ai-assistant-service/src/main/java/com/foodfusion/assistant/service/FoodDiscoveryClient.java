package com.foodfusion.assistant.service;

import com.foodfusion.assistant.dto.FoodItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FoodDiscoveryClient {
    private final RestClient restClient;
    private final String foodServiceUrl;

    public FoodDiscoveryClient(
            RestClient.Builder builder,
            @Value("${foodfusion.food-service-url}") String foodServiceUrl
    ) {
        this.restClient = builder.build();
        this.foodServiceUrl = foodServiceUrl;
    }

    public List<FoodItem> search(String query) {
        FoodItem[] items = restClient.get()
                .uri(foodServiceUrl + "/api/food/search?q={query}", query)
                .retrieve()
                .body(FoodItem[].class);
        return items == null ? List.of() : List.of(items);
    }
}
