package com.foodfusion.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodfusion.assistant.dto.ChatResponse;
import com.foodfusion.assistant.dto.FoodItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Service
public class AiAssistantService {
    private final FoodDiscoveryClient foodDiscovery;
    private final RestClient restClient;
    private final String providerEndpoint;
    private final String providerApiKey;
    private final String model;

    public AiAssistantService(
            FoodDiscoveryClient foodDiscovery,
            RestClient.Builder builder,
            @Value("${foodfusion.ai.endpoint:}") String providerEndpoint,
            @Value("${foodfusion.ai.api-key:}") String providerApiKey,
            @Value("${foodfusion.ai.model:}") String model
    ) {
        this.foodDiscovery = foodDiscovery;
        this.restClient = builder.build();
        this.providerEndpoint = providerEndpoint;
        this.providerApiKey = providerApiKey;
        this.model = model;
    }

    public ChatResponse chat(String message) {
        String query = message.trim();
        List<FoodItem> foods = foodDiscovery.search(query);
        if (StringUtils.hasText(providerEndpoint)) {
            return new ChatResponse(generateReply(query, foods), "llm", foods);
        }
        String reply = foods.isEmpty()
                ? "I couldn't find a menu item matching that request. Try a food name or category such as pasta, pizza, or dessert."
                : "Here are menu items matching your request. Availability and prices are shown from the Food Service.";
        return new ChatResponse(reply, "catalog-search", foods);
    }

    private String generateReply(String message, List<FoodItem> foods) {
        if (!StringUtils.hasText(providerApiKey) || !StringUtils.hasText(model)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The configured AI provider requires AI_PROVIDER_API_KEY and AI_MODEL.");
        }
        String menuContext = foods.stream()
                .map(food -> food.name() + " (" + food.category() + ", " + food.price() + ")")
                .reduce((left, right) -> left + "; " + right)
                .orElse("No matching menu items were found.");
        JsonNode response = restClient.post()
                .uri(providerEndpoint)
                .header("Authorization", "Bearer " + providerApiKey)
                .body(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content",
                                        "Help customers discover food using only this menu context: " + menuContext),
                                Map.of("role", "user", "content", message)
                        )
                ))
                .retrieve()
                .body(JsonNode.class);
        String content = response == null
                ? null
                : response.path("choices").path(0).path("message").path("content").asText(null);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("AI provider returned a response without assistant text.");
        }
        return content;
    }
}
