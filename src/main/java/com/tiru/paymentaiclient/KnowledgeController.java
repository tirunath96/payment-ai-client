package com.tiru.paymentaiclient;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final PaymentKnowledgeService knowledgeService;

    public KnowledgeController(PaymentKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam String query) {
        return knowledgeService.searchSimilarIncidents(query)
                .stream()
                .map(Document::getText)
                .toList();
    }
}