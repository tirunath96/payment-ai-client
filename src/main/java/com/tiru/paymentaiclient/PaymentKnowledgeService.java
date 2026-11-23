
package com.tiru.paymentaiclient;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import org.springframework.boot.CommandLineRunner;

@Service
public class PaymentKnowledgeService implements CommandLineRunner {

    private final VectorStore vectorStore;

    public PaymentKnowledgeService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        loadKnowledge();
        System.out.println("Historical payment incidents loaded into Vector Store.");
    }

    public List<Document> searchSimilarIncidents(String query) {
        return vectorStore.similaritySearch(query);
    }

    public void loadKnowledge() {

        List<Document> incidents = List.of(
                Document.builder()
                        .id(UUID.randomUUID().toString())
                        .text("Payment failed, customer account debited, gateway timed out. Verify gateway settlement before initiating refund.")
                        .build(),

                Document.builder()
                        .id(UUID.randomUUID().toString())
                        .text("Payment failed because bank declined the transaction. Customer account was not debited. Ask customer to retry later.")
                        .build(),

                Document.builder()
                        .id(UUID.randomUUID().toString())
                        .text("Payment pending for a long time due to gateway processing delay. Check gateway status before taking action.")
                        .build(),

                Document.builder()
                        .id(UUID.randomUUID().toString())
                        .text("Payment successful but merchant did not receive confirmation. Verify bank and gateway transaction references.")
                        .build());

        vectorStore.add(incidents);
    }
}
