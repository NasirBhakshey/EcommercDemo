package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.InventoryFailedEvent;
import com.ecommerce.notificationservice.service.EmailService;
import com.ecommerce.notificationservice.service.IdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryFailedConsumer {

    private final EmailService emailService;
    private final IdempotencyService idempotencyService;

    public InventoryFailedConsumer(EmailService emailService,
                                   IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.emailService = emailService;
    }

    @KafkaListener(
            topics = "inventory-failed",
            groupId = "notification-inventory-failed-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.notificationservice.events.InventoryFailedEvent"
            }
    )
    @Transactional
    public void consume(InventoryFailedEvent event) {

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "INVENTORY_FAILED",
                "notification-inventory-failed-group"
        );

        if (!claimed) {
            System.out.println(
                    "Duplicate INVENTORY_FAILED ignored. eventId="
                            + event.getEventId()
            );
            return;
        }

        System.out.println(
                "Processing INVENTORY_FAILED notification. eventId="
                        + event.getEventId()
        );

        emailService.sendInventoryFailedEmail(event);
    }
}
