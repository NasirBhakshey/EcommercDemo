package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.PaymentRefundedEvent;
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
public class PaymentRefundedConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentRefundedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentRefundedConsumer(
                inventoryService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldRestoreStockAfterOrderCancellationForNewRefundEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentRefundedEvent event = new PaymentRefundedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_REFUNDED",
                "product-payment-refunded-group"
        )).thenReturn(true);

        when(inventoryService
                .restoreStockAfterOrderCancellation(100L))
                .thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_REFUNDED",
                "product-payment-refunded-group"
        );

        verify(inventoryService)
                .restoreStockAfterOrderCancellation(100L);
    }

    @Test
    void consume_shouldIgnoreDuplicatePaymentRefundedEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentRefundedEvent event = new PaymentRefundedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_REFUNDED",
                "product-payment-refunded-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_REFUNDED",
                "product-payment-refunded-group"
        );

        verifyNoInteractions(inventoryService);
    }
}
