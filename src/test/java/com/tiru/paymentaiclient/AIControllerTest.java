
package com.tiru.paymentaiclient;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AIController.class)
class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AIService aiService;

    @Test
    void shouldReturnHighRiskForFailedTransaction() throws Exception {

        when(aiService.investigate("Investigate TXN1001", "test-001"))
                .thenReturn(new InvestigationResponse(
                        "TXN1001", "HIGH RISK",
                        "Transaction failed and account debited."));

        mockMvc.perform(post("/api/ai/investigate")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "message": "Investigate TXN1001",
                            "conversationId": "test-001"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN1001"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH RISK"))
                .andExpect(jsonPath("$.analysis").exists());
    }

    @Test
    void shouldReturnNormalForSuccessfulTransaction() throws Exception {

        when(aiService.investigate("Investigate TXN1002", "test-002"))
                .thenReturn(new InvestigationResponse(
                        "TXN1002", "NORMAL", "Transaction successful."));

        mockMvc.perform(post("/api/ai/investigate")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "message": "Investigate TXN1002",
                            "conversationId": "test-002"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN1002"))
                .andExpect(jsonPath("$.riskLevel").value("NORMAL"));
    }

    @Test
    void shouldReturnValidationResponseForMissingTransactionId()
            throws Exception {

        when(aiService.investigate("Hello", "test-003"))
                .thenReturn(new InvestigationResponse(
                        "UNKNOWN", "UNKNOWN",
                        "Please provide a transaction ID, for example TXN1001."));

        mockMvc.perform(post("/api/ai/investigate")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                            "message": "Hello",
                            "conversationId": "test-003"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("UNKNOWN"))
                .andExpect(jsonPath("$.riskLevel").value("UNKNOWN"))
                .andExpect(jsonPath("$.analysis").value(
                        "Please provide a transaction ID, for example TXN1001."));
    }
}
