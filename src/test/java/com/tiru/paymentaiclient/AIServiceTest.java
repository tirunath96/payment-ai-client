
package com.tiru.paymentaiclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIServiceTest {

    private AIService aiService;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        SyncMcpToolCallbackProvider mcpTools =
                mock(SyncMcpToolCallbackProvider.class);

        when(builder.build()).thenReturn(chatClient);

        aiService = new AIService(builder, mcpTools);
    }

    @Test
    void failedAndDebitedShouldBeHighRisk() {
        assertEquals("HIGH RISK",
                aiService.classifyRisk(
                        "Status: FAILED, Account debit status: DEBITED",
                        "Gateway response: TIMEOUT"));
    }

    @Test
    void successfulAndApprovedShouldBeNormal() {
        assertEquals("NORMAL",
                aiService.classifyRisk(
                        "Status: SUCCESS, Account debit status: DEBITED",
                        "Gateway response: APPROVED"));
    }

    @Test
    void pendingShouldRequireInvestigation() {
        assertEquals("INVESTIGATION REQUIRED",
                aiService.classifyRisk(
                        "Status: PENDING, Account debit status: NOT DEBITED",
                        "Gateway response: PROCESSING"));
    }

    @Test
    void gatewayTimeoutShouldRequireInvestigation() {
        assertEquals("INVESTIGATION REQUIRED",
                aiService.classifyRisk(
                        "Status: FAILED, Account debit status: NOT DEBITED",
                        "Gateway response: TIMEOUT"));
    }
}
