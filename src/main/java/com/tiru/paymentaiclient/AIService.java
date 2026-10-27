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
            String riskLevel = classifyRisk(transactionDetails, gatewayDetails);
            return chatClient.prompt()
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
    If records are missing, clearly state that the transaction
    cannot be verified.

    Risk classification:
    - HIGH RISK: Transaction FAILED while account status is DEBITED.
    - INVESTIGATION REQUIRED: Gateway TIMEOUT or transaction PENDING.
    - NORMAL: Transaction SUCCESS and gateway response APPROVED.
    - If multiple conditions apply, prioritize HIGH RISK.
    - Explain the reason for the classification.
    - Do not treat this classification as proof of fraud.
    """)
                    .user("""
    Investigate transaction %s.

    CUSTOMER CONCERN:
    %s

    BANK TRANSACTION:
    %s

    PAYMENT GATEWAY:
    %s

    Analyze the customer's concern using the supplied evidence.
    Explain discrepancies and recommend safe next steps.
    Include both bank and gateway details.
    Never invent facts or claim a refund was executed.
    JAVA-CALCULATED RISK LEVEL: %s
    """.formatted(
        transactionId,
        question,
        transactionDetails,
        gatewayDetails,
        riskLevel))
                    .call()
                    .content();

        } catch (Exception e) {
            System.err.println("Investigation failed: " + e.getMessage());
            return "Unable to investigate transaction "
                    + transactionId + " due to a system error.";
        }
    }

    private String classifyRisk(String transactionDetails, String gatewayDetails) {
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