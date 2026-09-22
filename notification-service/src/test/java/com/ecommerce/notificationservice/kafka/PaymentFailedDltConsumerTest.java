package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class PaymentFailedDltConsumerTest {

    private PaymentFailedDltConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentFailedDltConsumer();
    }

    @Test
    void consume_shouldHandlePaymentFailedDltEvent() {

        PaymentFailedEvent event = new PaymentFailedEvent();

        event.setEventId(UUID.randomUUID());
        event.setOrderId(100L);
        event.setEmail("test@example.com");
        event.setReason("Payment processing failed");

        assertDoesNotThrow(
                () -> consumer.consume(event)
        );
    }
}
