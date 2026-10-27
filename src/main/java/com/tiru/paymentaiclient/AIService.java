
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

    public InvestigationResponse investigate(String question) {

        if (question == null || question.isBlank()) {
            return new InvestigationResponse(
                    "UNKNOWN", "UNKNOWN",
                    "Please provide a transaction ID, for example TXN1001.");
        }

        Matcher matcher = Pattern.compile("(?i)\\bTXN\\d+\\b")
                .matcher(question);

        if (!matcher.find()) {
            return new InvestigationResponse(
                    "UNKNOWN", "UNKNOWN",
                    "Please provide a valid transaction ID, for example TXN1001.");
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
                return new InvestigationResponse(
                        transactionId, "UNKNOWN",
                        "Required MCP payment tools were not available.");
            }

            if (transactionDetails.startsWith("No transaction found")) {
                return new InvestigationResponse(
                        transactionId, "UNKNOWN", transactionDetails);
            }

            if (gatewayDetails.startsWith("No gateway response found")) {
                return new InvestigationResponse(
                        transactionId, "UNKNOWN", gatewayDetails);
            }

            String riskLevel =
                    classifyRisk(transactionDetails, gatewayDetails);

            String analysis = chatClient.prompt()
                    .system("""
                        You are a banking payment investigation assistant.

                        Use only supplied database records as verified facts.
                        Distinguish confirmed facts from possible explanations.
                        A gateway TIMEOUT does not prove the merchant server failed.
                        If the account is DEBITED and the transaction FAILED or uncertain,
                        do not recommend an immediate retry.
                        Recommend checking transaction status and reconciliation records first.
                        Never claim a refund or reversal occurred unless verified.
                        Explain discrepancies clearly and recommend safe next steps.
                        Always include transaction status, debit status, gateway response,
                        and gateway reference when available.
                        Do not treat risk classification as proof of fraud.
                        """)
                    .user("""
                        Investigate transaction %s.

                        CUSTOMER CONCERN:
                        %s

                        BANK TRANSACTION:
                        %s

                        PAYMENT GATEWAY:
                        %s

                        JAVA-CALCULATED RISK LEVEL: %s

                        Explain the evidence, discrepancies, and safe next steps.
                        Do not invent facts or claim a refund was executed.
                        """.formatted(
                            transactionId,
                            question,
                            transactionDetails,
                            gatewayDetails,
                            riskLevel))
                    .call()
                    .content();

            return new InvestigationResponse(
                    transactionId, riskLevel, analysis);

        } catch (Exception e) {
            System.err.println("Investigation failed: " + e.getMessage());
            return new InvestigationResponse(
                    transactionId, "UNKNOWN",
                    "Unable to investigate this transaction due to a system error.");
        }
    }

    public String classifyRisk(
            String transactionDetails, String gatewayDetails) {

        String transaction = transactionDetails.toUpperCase();
        String gateway = gatewayDetails.toUpperCase();

        boolean debited = transaction.contains("DEBITED")
                && !transaction.contains("NOT DEBITED");

        if (transaction.contains("FAILED") && debited) {
            return "HIGH RISK";
        }

        if (transaction.contains("PENDING") || gateway.contains("TIMEOUT")) {
            return "INVESTIGATION REQUIRED";
        }

        if (transaction.contains("SUCCESS") && gateway.contains("APPROVED")) {
            return "NORMAL";
        }

        return "INVESTIGATION REQUIRED";
    }
}
