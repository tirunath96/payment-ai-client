package com.tiru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.tiru", "com.tiru.paymentaiclient"})
public class PaymentAiClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentAiClientApplication.class, args);
    }
}