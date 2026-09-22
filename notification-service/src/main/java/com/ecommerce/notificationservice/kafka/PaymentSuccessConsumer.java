package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentSuccessEvent;
import com.ecommerce.notificationservice.service.EmailService;
import com.ecommerce.notificationservice.service.IdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentSuccessConsumer {

    private final EmailService emailService;
    private final IdempotencyService idempotencyService;

    public PaymentSuccessConsumer(EmailService emailService,
                                  IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.emailService = emailService;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-success",
            groupId = "notification-payment-success-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.notificationservice.events.PaymentSuccessEvent"
            }
    )
    public void consume(PaymentSuccessEvent event) {

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_SUCCESS",
                "notification-payment-success-group"
        );

        if (!claimed) {
            System.out.println("Duplicate PAYMENT_SUCCESS ignored. eventId=" + event.getEventId());
            return;
        }

        System.out.println("Processing PAYMENT_SUCCESS notification. eventId=" + event.getEventId());

        emailService.sendPaymentSuccessEmail(event);
    }
}
