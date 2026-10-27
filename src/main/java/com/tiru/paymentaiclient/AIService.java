package com.tiru.paymentaiclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpTools;

    public AIService(
            ChatClient.Builder builder,
            SyncMcpToolCallbackProvider mcpTools) {
        this.chatClient = builder.build();
        this.mcpTools = mcpTools;
    }

    public String investigate(String question) {

        if (question == null || question.isBlank()) {
            return "Please provide a transaction ID, for example TXN1001.";
        }

        Matcher matcher = Pattern.compile("(?i)\\bTXN\\d+\\b")
                .matcher(question);

        if (!matcher.find()) {
            return "Please provide a valid transaction ID, for example TXN1001.";
        }

        String transactionId = matcher.group().toUpperCase();

        String transactionDetails = null;
        String gatewayDetails = null;

        try {
            for (ToolCallback tool : mcpTools.getToolCallbacks()) {

                String toolName = tool.getToolDefinition().name();

                String input = "{\"transactionId\":\""
                        + transactionId + "\"}";

                if (toolName.equals("getPaymentStatus")) {
                    transactionDetails = tool.call(input);
                } else if (toolName.equals("getGatewayStatus")) {
                    gatewayDetails = tool.call(input);
                }
            }

            if (transactionDetails == null || gatewayDetails == null) {
                return "Required MCP payment tools were not available.";
            }

            if (transactionDetails.startsWith("No transaction found")) {
                return transactionDetails;
            }

            if (gatewayDetails.startsWith("No gateway response found")) {
                return gatewayDetails;
            }

            return chatClient.prompt()
                    .system("""
                        You are a banking payment investigation assistant.
                        Analyze only the supplied evidence.
                        Explain transaction status, debit status,
                        gateway response, gateway reference,
                        discrepancies, and recommended next steps.
                        Never invent information or claim a refund was executed.
                        """)
                    .user("""
                        Investigate transaction %s.

                        BANK TRANSACTION:
                        %s

                        PAYMENT GATEWAY:
                        %s

                        Include both bank and gateway details in your report.
                        """.formatted(
                            transactionId,
                            transactionDetails,
                            gatewayDetails))
                    .call()
                    .content();

        } catch (Exception e) {
            System.err.println("Investigation failed: " + e.getMessage());
            return "Unable to investigate transaction "
                    + transactionId + " due to a system error.";
        }
    }
}