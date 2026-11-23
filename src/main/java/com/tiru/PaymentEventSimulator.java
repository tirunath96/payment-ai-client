
package com.tiru;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class PaymentEventSimulator {

    public PaymentEvent generateEvent() {
        String[] statuses = {"FAILED", "PENDING", "SUCCESS"};
        String status = statuses[ThreadLocalRandom.current()
                .nextInt(statuses.length)];

        String failureCode = "FAILED".equals(status)
                ? "GATEWAY_TIMEOUT"
                : null;

        return new PaymentEvent(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("2500.00"),
                status,
                "Gateway-A",
                failureCode,
                LocalDateTime.now()
        );
    }
}
