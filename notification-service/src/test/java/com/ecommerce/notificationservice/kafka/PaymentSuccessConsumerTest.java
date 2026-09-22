package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentSuccessEvent;
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
public class PaymentSuccessConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentSuccessConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentSuccessConsumer(
                emailService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldSendEmailForNewPaymentSuccessEvent() {

        UUID eventId = UUID.randomUUID();

        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "notification-payment-success-group"
        )).thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "notification-payment-success-group"
        );

        verify(emailService)
                .sendPaymentSuccessEmail(event);
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
                "notification-payment-success-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "PAYMENT_SUCCESS",
                "notification-payment-success-group"
        );

        verifyNoInteractions(emailService);
    }
}
