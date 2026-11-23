
package com.tiru;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class PaymentEventController {

    private final PaymentEventSimulator simulator;
    private final PaymentEventProducer producer;

    public PaymentEventController(
            PaymentEventSimulator simulator,
            PaymentEventProducer producer) {
        this.simulator = simulator;
        this.producer = producer;
    }

    @PostMapping("/simulate")
    public PaymentEvent simulateEvent() throws Exception {
        PaymentEvent event = simulator.generateEvent();
        producer.publish(event);
        return event;
    }
}
