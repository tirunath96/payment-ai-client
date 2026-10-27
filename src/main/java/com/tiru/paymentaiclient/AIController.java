package com.tiru.paymentaiclient;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/investigate")
    public String investigate(@RequestBody ChatRequest request) {
        return aiService.investigate(request.message());
    }

    public record ChatRequest(String message) {}
}