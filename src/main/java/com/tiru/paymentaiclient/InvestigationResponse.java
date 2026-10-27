package com.tiru.paymentaiclient;

public record InvestigationResponse(
        String transactionId,
        String riskLevel,
        String analysis
) {}