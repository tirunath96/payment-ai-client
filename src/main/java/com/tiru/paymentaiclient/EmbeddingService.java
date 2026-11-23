package com.tiru.paymentaiclient;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }

    @PostConstruct
    public void testEmbedding() {
        float[] vector = generateEmbedding(
                "Payment failed but customer account was debited");

        System.out.println("Embedding dimensions: " + vector.length);
    }
}