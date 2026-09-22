package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.PaymentFailedEvent;
import com.ecommerce.productservice.service.IdempotencyService;
import com.ecommerce.productservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentFailedConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentFailedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentFailedConsumer(
                inventoryService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldRestoreStockForNewPaymentFailedEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "product-payment-failed-group"
        )).thenReturn(true);

        when(inventoryService.restoreStock(100L))
                .thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "product-payment-failed-group"
        );

        verify(inventoryService)
                .restoreStock(100L);
    }

    @Test
    void consume_shouldIgnoreDuplicatePaymentFailedEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "product-payment-failed-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "product-payment-failed-group"
        );

        verifyNoInteractions(inventoryService);
    }
}
