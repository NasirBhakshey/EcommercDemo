package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.PaymentSuccessEvent;
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
public class PaymentSuccessConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentSuccessConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentSuccessConsumer(
                inventoryService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldCommitStockForNewPaymentSuccessEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "product-payment-success-group"
        )).thenReturn(true);

        when(inventoryService.commitStock(100L))
                .thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "product-payment-success-group"
        );

        verify(inventoryService).commitStock(100L);
    }

    @Test
    void consume_shouldIgnoreDuplicatePaymentSuccessEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "product-payment-success-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "product-payment-success-group"
        );

        verifyNoInteractions(inventoryService);
    }
}
