package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentSuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class PaymentSuccessDltConsumerTest {

    private PaymentSuccessDltConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentSuccessDltConsumer();
    }

    @Test
    void consume_shouldHandlePaymentSuccessDltEvent() {

        PaymentSuccessEvent event = new PaymentSuccessEvent();

        event.setEventId(UUID.randomUUID());
        event.setOrderId(100L);
        event.setEmail("test@example.com");

        assertDoesNotThrow(
                () -> consumer.consume(event)
        );
    }
}
