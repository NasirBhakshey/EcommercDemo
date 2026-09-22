package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.PaymentFailedEvent;
import com.ecommerce.orderservice.event.PaymentSuccessEvent;
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
public class PaymentEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentEventConsumer(
                orderService,
                idempotencyService
        );
    }

    // =========================================================
    // PAYMENT SUCCESS
    // =========================================================

    @Test
    void consumeSuccess_shouldMarkPaymentSuccess_whenEventIsNew() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        PaymentSuccessEvent event = new PaymentSuccessEvent(
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
                "PAYMENT_SUCCESS",
                "order-payment-success-group"
        )).thenReturn(true);

        // Act
        consumer.consumeSuccess(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "PAYMENT_SUCCESS",
                        "order-payment-success-group"
                );

        verify(orderService, times(1))
                .markPaymentSuccess(orderId);
    }

    @Test
    void consumeSuccess_shouldIgnoreEvent_whenEventIsDuplicate() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        PaymentSuccessEvent event = new PaymentSuccessEvent(
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
                "PAYMENT_SUCCESS",
                "order-payment-success-group"
        )).thenReturn(false);

        // Act
        consumer.consumeSuccess(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "PAYMENT_SUCCESS",
                        "order-payment-success-group"
                );

        verify(orderService, never())
                .markPaymentSuccess(anyLong());

        verifyNoMoreInteractions(orderService);
    }

    // =========================================================
    // PAYMENT FAILED
    // =========================================================

    @Test
    void consumeFailure_shouldMarkPaymentFailed_whenEventIsNew() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                500L,
                orderId,
                10L,
                new BigDecimal("20000.00"),
                "Payment declined",
                LocalDateTime.now()
        );

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "order-payment-failed-group"
        )).thenReturn(true);

        // Act
        consumer.consumeFailure(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "PAYMENT_FAILED",
                        "order-payment-failed-group"
                );

        verify(orderService, times(1))
                .markPaymentFailed(orderId);
    }

    @Test
    void consumeFailure_shouldIgnoreEvent_whenEventIsDuplicate() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                500L,
                orderId,
                10L,
                new BigDecimal("20000.00"),
                "Payment declined",
                LocalDateTime.now()
        );

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "order-payment-failed-group"
        )).thenReturn(false);

        // Act
        consumer.consumeFailure(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "PAYMENT_FAILED",
                        "order-payment-failed-group"
                );

        verify(orderService, never())
                .markPaymentFailed(anyLong());

        verifyNoMoreInteractions(orderService);
    }

}
