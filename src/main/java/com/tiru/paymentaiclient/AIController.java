package com.tiru.paymentaiclient;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/investigate")
    public InvestigationResponse investigate(@RequestBody ChatRequest request) {
        String conversationId = request.conversationId() == null
                ? "default"
                : request.conversationId();

        return aiService.investigate(request.message(), conversationId);
    }

    public record ChatRequest(String message, String conversationId) {}
}