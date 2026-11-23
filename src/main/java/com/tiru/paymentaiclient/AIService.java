
package com.tiru.paymentaiclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.stream.Collectors;
import org.springframework.ai.document.Document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpTools;
    private final PaymentKnowledgeService knowledgeService;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;
    private final Map<String, String> activeTransactions = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    public AIService(
            ChatClient.Builder builder,
            SyncMcpToolCallbackProvider mcpTools,
            PaymentKnowledgeService knowledgeService,
            MessageChatMemoryAdvisor chatMemoryAdvisor) {

        this.mcpTools = mcpTools;
        this.knowledgeService = knowledgeService;
        this.chatMemoryAdvisor = chatMemoryAdvisor;
        this.chatClient = builder.defaultAdvisors(chatMemoryAdvisor).build();
    }

    public InvestigationResponse investigate(String question, String conversationId) {

        if (question == null || question.isBlank()) {
            return new InvestigationResponse(
                    "UNKNOWN", "UNKNOWN",
                    "Please provide a transaction ID, for example TXN1001.");
        }

        Matcher matcher = Pattern.compile("(?i)\\bTXN\\d+\\b")
                .matcher(question);

        String transactionId;

        if (matcher.find()) {
            transactionId = matcher.group().toUpperCase();
            activeTransactions.put(conversationId, transactionId);
        } else {
            transactionId = activeTransactions.get(conversationId);
        }

        if (transactionId == null) {
            return new InvestigationResponse(
                    "UNKNOWN", "UNKNOWN",
                    "Please provide a transaction ID, for example TXN1001.");
        }

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

            String riskLevel = classifyRisk(transactionDetails, gatewayDetails);

            String riskExplanation;

            if (riskLevel.equals("HIGH RISK")) {
                riskExplanation = """
                        CRITICAL DISCREPANCY:
                        The bank transaction status is FAILED.
                        The customer's account debit status is DEBITED.
                        This means the account debited despite FAILED status.
                        These two bank fields are inconsistent.
                        This is NOT a successful payment.
                        """;
            } else if (riskLevel.equals("NORMAL")) {
                riskExplanation = """
                        The bank transaction status is SUCCESS
                        and the gateway response is APPROVED.
                        Both records indicate successful processing.
                        """;
            } else {
                riskExplanation = """
                        The transaction requires further investigation.
                        Do not assume the final payment outcome.
                        A TIMEOUT means the gateway outcome is unknown.
                        """;
            }

            String retrievalQuery = question + "\n"
                    + transactionDetails + "\n"
                    + gatewayDetails;

            String historicalContext = knowledgeService
                    .searchSimilarIncidents(retrievalQuery)
                    .stream()
                    .limit(3)
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));

            String analysis = chatClient.prompt()

                    .system("""
                                You are an expert banking payment operations investigator.

                                YOUR ROLE:
                                Analyze payment transaction evidence and explain the findings.
                                You are an AI analyst, not the source of transaction data.

                                SOURCE OF TRUTH:
                                - Bank transaction and gateway records are the only sources
                                  of transaction facts.
                                - Java calculates the risk classification.
                                - The supplied JAVA-CALCULATED RISK LEVEL is authoritative.
                                - Never say that the gateway or AI calculated the risk.
                                - HIGH RISK means the current Java rule detected a FAILED
                                  transaction with a DEBITED account.
                                - HIGH RISK does not mean fraud is confirmed.
                                - Do not invent missing transaction details.

                                ANALYSIS RULES:
                                - Compare bank status, debit status and gateway response.
                                - Identify agreement or disagreement between the records.
                                - Explain what is confirmed and what remains unknown.
                                - A TIMEOUT does not establish whether the payment succeeded
                                  or failed.
                                - Do not assume fraud, unusual activity or compliance violations.
                                - Do not invent settlement, refund or reversal outcomes.

                                - A bank status of FAILED is a bank-recorded status only.
                                - A gateway response of TIMEOUT means the final gateway
                                  outcome is UNKNOWN.
                                - Never describe TIMEOUT as confirmation of payment failure.
                                - Never claim both systems agree when one system reports TIMEOUT.
                                - Do not infer settlement, success, failure or reversal
                                  from a TIMEOUT response.
                                - Recommend checking settlement and reconciliation records.
                                  Do not recommend initiating settlement.
                                - Clearly state when the final payment outcome is unknown.


                                - FAILED + DEBITED is an inconsistent payment outcome:
                                the bank records show failure, but the customer's account
                                was debited.
                                - Never describe this combination as successful payment
                                processing or successful transaction completion.
                                - Use the exact phrase "account debited despite FAILED status".
                                - Do not say bank and gateway records are aligned when
                                the gateway response is TIMEOUT.
                                - Avoid repeating the same recommendation in multiple sections.
                                - Prioritize recommendations in this order:
                                1. Verify final gateway outcome.
                                2. Check settlement and reconciliation records.
                                3. Decide on refund, reversal or other action only after verification.



                                DECISION GUIDANCE:
                                - FAILED + DEBITED: Recommend settlement and reconciliation
                                  verification before any refund or reversal.
                                - PENDING or TIMEOUT: Recommend checking the final gateway
                                  status before retrying.
                                - SUCCESS + APPROVED: Explain that both records indicate success.
                                - Missing or conflicting records: Recommend manual investigation.
                                - Never recommend retrying a payment without checking whether
                                  the original payment was processed or debited.

                                RESPONSE FORMAT:
                                1. Investigation Summary
                                2. Verified Evidence
                                3. Findings
                                4. Recommended Next Steps

                                RESPONSE QUALITY:
                                - Be concise, specific and professional.
                                - Separate facts from possibilities.
                                - Explain why each recommendation is necessary.
                                - Never claim an action has already been performed.
                                - If evidence is insufficient, explicitly say so.

                                HISTORICAL KNOWLEDGE RULES:
                            - Historical incidents are reference examples, not current transaction evidence.
                            - Never assume a historical incident happened to the current customer.
                            - Use relevant historical patterns to explain possible causes.
                            - Never override verified bank or gateway records with historical information.
                                """)

                    .user("""
                            Investigate transaction %s.

                            CUSTOMER CONCERN:
                            %s

                            BANK TRANSACTION:
                            %s

                            PAYMENT GATEWAY:
                            %s

                            HISTORICAL REFERENCE INCIDENTS:
                            %s

                            JAVA-CALCULATED RISK LEVEL: %s

                            JAVA-CALCULATED RISK EXPLANATION:
%s

                            Explain the evidence, discrepancies, and safe next steps.
                            Do not invent facts or claim a refund was executed.
                            """.formatted(
                            transactionId,
                            question,
                            transactionDetails,
                            gatewayDetails,
                            historicalContext,
                            riskLevel,
                            riskExplanation))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();

            return new InvestigationResponse(
                    transactionId, riskLevel, analysis);

        } catch (Exception e) {
            logger.error("Payment investigation failed for transactionId: {}", transactionId, e);
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
