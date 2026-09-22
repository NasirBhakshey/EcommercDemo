package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.PaymentRefundedEvent;
import com.ecommerce.orderservice.service.IdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentRefundedConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentRefundedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentRefundedConsumer(
                orderService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldMarkPaymentRefunded_whenEventIsNew() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        PaymentRefundedEvent event = new PaymentRefundedEvent(
                eventId,
                500L,
                orderId,
                10L,
                new BigDecimal("20000.00"),
                "TXN-10001",
                LocalDateTime.now()
        );

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_REFUNDED",
                "order-payment-refunded-group"
        )).thenReturn(true);

        // Act
        consumer.consume(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "PAYMENT_REFUNDED",
                        "order-payment-refunded-group"
                );

        verify(orderService, times(1))
                .markPaymentRefunded(orderId);
    }

    @Test
    void consume_shouldIgnoreEvent_whenEventIsDuplicate() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        PaymentRefundedEvent event = new PaymentRefundedEvent(
                eventId,
                500L,
                orderId,
                10L,
                new BigDecimal("20000.00"),
                "TXN-10001",
                LocalDateTime.now()
        );

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_REFUNDED",
                "order-payment-refunded-group"
        )).thenReturn(false);

        // Act
        consumer.consume(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "PAYMENT_REFUNDED",
                        "order-payment-refunded-group"
                );

        verify(orderService, never())
                .markPaymentRefunded(anyLong());

        verifyNoMoreInteractions(orderService);
    }
}
