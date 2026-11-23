
package com.tiru.paymentaiclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class AIServiceTest {

    private AIService aiService;
    private SyncMcpToolCallbackProvider mcpTools;
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        mcpTools = mock(SyncMcpToolCallbackProvider.class);

        PaymentKnowledgeService knowledgeService =
                mock(PaymentKnowledgeService.class);

        MessageChatMemoryAdvisor memoryAdvisor =
                mock(MessageChatMemoryAdvisor.class);

        when(builder.defaultAdvisors(memoryAdvisor)).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        aiService = new AIService(
                builder, mcpTools, knowledgeService, memoryAdvisor);
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
    void blankQuestionShouldReturnValidationMessage() {
        InvestigationResponse response =
                aiService.investigate(" ", "test-001");

        assertEquals("UNKNOWN", response.riskLevel());
        assertEquals(
                "Please provide a transaction ID, for example TXN1001.",
                response.analysis());
    }

    @Test
    void invalidTransactionIdShouldReturnValidationMessage() {
        InvestigationResponse response =
                aiService.investigate("Investigate ABC123", "test-001");

        assertEquals("UNKNOWN", response.riskLevel());
        assertEquals(
                "Please provide a transaction ID, for example TXN1001.",
                response.analysis());
    }

    @Test
    void gatewayTimeoutShouldRequireInvestigation() {
        assertEquals("INVESTIGATION REQUIRED",
                aiService.classifyRisk(
                        "Status: FAILED, Account debit status: NOT DEBITED",
                        "Gateway response: TIMEOUT"));
    }

    @Test
    void mcpFailureShouldReturnSafeErrorMessage() {
        when(mcpTools.getToolCallbacks())
                .thenThrow(new RuntimeException("MCP unavailable"));

        InvestigationResponse response =
                aiService.investigate("Investigate TXN1001", "test-001");

        assertEquals("UNKNOWN", response.riskLevel());
        assertEquals(
                "Unable to investigate this transaction due to a system error.",
                response.analysis());
    }

    @Test
    void missingTransactionShouldReturnSafeMessage() {
        ToolCallback paymentTool = mock(ToolCallback.class);
        ToolCallback gatewayTool = mock(ToolCallback.class);

        when(mcpTools.getToolCallbacks())
                .thenReturn(new ToolCallback[]{paymentTool, gatewayTool});

        when(paymentTool.getToolDefinition())
                .thenReturn(mock(ToolDefinition.class));
        when(gatewayTool.getToolDefinition())
                .thenReturn(mock(ToolDefinition.class));

        when(paymentTool.getToolDefinition().name())
                .thenReturn("getPaymentStatus");
        when(gatewayTool.getToolDefinition().name())
                .thenReturn("getGatewayStatus");

        when(paymentTool.call("{\"transactionId\":\"TXN9999\"}"))
                .thenReturn("No transaction found for TXN9999");

        when(gatewayTool.call("{\"transactionId\":\"TXN9999\"}"))
                .thenReturn("No gateway response found for TXN9999");

        InvestigationResponse response =
                aiService.investigate("Investigate TXN9999", "test-001");

        assertEquals("UNKNOWN", response.riskLevel());
        assertEquals(
                "No transaction found for TXN9999",
                response.analysis());

        verifyNoInteractions(chatClient);
    }
}
