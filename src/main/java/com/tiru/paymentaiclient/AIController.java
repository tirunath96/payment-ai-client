package com.tiru.paymentaiclient;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5174")
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/investigate")
    public InvestigationResponse investigate(@RequestBody ChatRequest request) {
    return aiService.investigate(request.message());
}
    public record ChatRequest(String message) {}
}