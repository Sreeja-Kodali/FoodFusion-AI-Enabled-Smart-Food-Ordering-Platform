package com.foodfusion.assistant.controller;

import com.foodfusion.assistant.dto.ChatRequest;
import com.foodfusion.assistant.dto.ChatResponse;
import com.foodfusion.assistant.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/assistant", "/api/ai"})
public class AiAssistantController {
    private final AiAssistantService assistant;

    public AiAssistantController(AiAssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return assistant.chat(request.message());
    }
}
