package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentFailedEvent;
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
public class PaymentFailedConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentFailedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentFailedConsumer(
                emailService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldSendEmailForNewPaymentFailedEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "notification-payment-failed-group"
        )).thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "notification-payment-failed-group"
        );

        verify(emailService)
                .sendPaymentFailedEmail(event);
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
                "notification-payment-failed-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_FAILED",
                "notification-payment-failed-group"
        );

        verifyNoInteractions(emailService);
    }
}
