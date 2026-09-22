package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.InventoryFailedEvent;
import com.ecommerce.notificationservice.service.EmailService;
import com.ecommerce.notificationservice.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryFailedConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private IdempotencyService idempotencyService;

    private InventoryFailedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryFailedConsumer(
                emailService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldSendEmailForNewInventoryFailedEvent() {

        UUID eventId = UUID.randomUUID();

        InventoryFailedEvent event = new InventoryFailedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "INVENTORY_FAILED",
                "notification-inventory-failed-group"
        )).thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "INVENTORY_FAILED",
                "notification-inventory-failed-group"
        );

        verify(emailService)
                .sendInventoryFailedEmail(event);
    }

    @Test
    void consume_shouldIgnoreDuplicateInventoryFailedEvent() {

        UUID eventId = UUID.randomUUID();

        InventoryFailedEvent event = new InventoryFailedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "INVENTORY_FAILED",
                "notification-inventory-failed-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "INVENTORY_FAILED",
                "notification-inventory-failed-group"
        );

        verifyNoInteractions(emailService);
    }
}
