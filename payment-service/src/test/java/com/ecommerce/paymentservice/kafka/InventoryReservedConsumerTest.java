package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.InventoryReservedEvent;
import com.ecommerce.paymentservice.service.IdempotencyService;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryReservedConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private IdempotencyService idempotencyService;

    private InventoryReservedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryReservedConsumer(
                paymentService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldProcessPaymentWhenEventIsNew() {

        UUID eventId = UUID.randomUUID();

        InventoryReservedEvent event = new InventoryReservedEvent();

        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "INVENTORY_RESERVED",
                "payment-inventory-reserved-group"
        )).thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "INVENTORY_RESERVED",
                "payment-inventory-reserved-group"
        );

        verify(paymentService).processPayment(event);
    }

    @Test
    void consume_shouldIgnoreDuplicateEvent() {

        UUID eventId = UUID.randomUUID();

        InventoryReservedEvent event =
                new InventoryReservedEvent();

        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "INVENTORY_RESERVED",
                "payment-inventory-reserved-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "INVENTORY_RESERVED",
                "payment-inventory-reserved-group"
        );

        verify(paymentService, never()).processPayment(any(InventoryReservedEvent.class));
    }

}
