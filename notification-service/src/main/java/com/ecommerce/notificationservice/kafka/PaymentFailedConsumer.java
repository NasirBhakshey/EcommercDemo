package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentFailedEvent;
import com.ecommerce.notificationservice.service.EmailService;
import com.ecommerce.notificationservice.service.IdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentFailedConsumer {

    private final EmailService emailService;
    private final IdempotencyService idempotencyService;

    public PaymentFailedConsumer(EmailService emailService,
                                 IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.emailService = emailService;
    }

    @KafkaListener(
            topics = "payment-failed",
            groupId = "notification-payment-failed-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.notificationservice.events.PaymentFailedEvent"
            }
    )
    @Transactional
    public void consume(PaymentFailedEvent event) {

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_FAILED",
                "notification-payment-failed-group"
        );

        if (!claimed) {
            System.out.println("Duplicate PAYMENT_FAILED ignored. eventId=" + event.getEventId());
            return;
        }

        System.out.println("Processing PAYMENT_FAILED notification. eventId=" + event.getEventId());

        emailService.sendPaymentFailedEmail(event);
    }
}
