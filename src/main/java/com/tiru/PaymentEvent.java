
package com.tiru;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        String transactionId,
        BigDecimal amount,
        String status,
        String gateway,
        String failureCode,
        LocalDateTime timestamp
) {}
