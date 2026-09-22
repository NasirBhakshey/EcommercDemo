package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.OrderCancelledEvent;
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
public class OrderCancelledConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private IdempotencyService idempotencyService;

    private OrderCancelledConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderCancelledConsumer(
                paymentService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldRefundPaymentWhenEventIsNew() {

        UUID eventId = UUID.randomUUID();

        OrderCancelledEvent event = new OrderCancelledEvent();

        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "ORDER_CANCELLED",
                "payment-order-cancelled-group"
        )).thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "ORDER_CANCELLED",
                "payment-order-cancelled-group"
        );

        verify(paymentService).refundPayment(100L);
    }

    @Test
    void consume_shouldIgnoreDuplicateEvent() {

        UUID eventId = UUID.randomUUID();

        OrderCancelledEvent event = new OrderCancelledEvent();

        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "ORDER_CANCELLED",
                "payment-order-cancelled-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "ORDER_CANCELLED",
                "payment-order-cancelled-group"
        );

        verify(paymentService, never()).refundPayment(anyLong());
    }
}
