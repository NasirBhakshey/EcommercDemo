package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.InventoryFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class InventoryFailedDltConsumerTest {

    private InventoryFailedDltConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryFailedDltConsumer();
    }

    @Test
    void consume_shouldHandleInventoryFailedDltEvent() {

        InventoryFailedEvent event = new InventoryFailedEvent();

        event.setEventId(UUID.randomUUID());
        event.setOrderId(100L);
        event.setEmail("test@example.com");
        event.setReason("Insufficient stock");

        assertDoesNotThrow(
                () -> consumer.consume(event)
        );
    }
}
