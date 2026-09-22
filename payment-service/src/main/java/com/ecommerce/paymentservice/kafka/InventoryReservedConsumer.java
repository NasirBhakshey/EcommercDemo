package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.InventoryReservedEvent;
import com.ecommerce.paymentservice.service.IdempotencyService;
import com.ecommerce.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryReservedConsumer {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public InventoryReservedConsumer(PaymentService paymentService,
                                     IdempotencyService idempotencyService){
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
    }

    @Transactional
    @KafkaListener(
            topics = "inventory-reserved",
            groupId = "payment-inventory-reserved-group",
            properties = {
                    "spring.json.value.default.type=com.ecommerce.paymentservice.event.InventoryReservedEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void consume(InventoryReservedEvent event) {

        final String consumerName = "payment-inventory-reserved-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "INVENTORY_RESERVED",
                consumerName
        );

        if (!claimed) {
            System.out.println("DUPLICATE INVENTORY_RESERVED EVENT IGNORED. Event ID: " + event.getEventId());
            return;
        }

        System.out.println("==============================");
        System.out.println("INVENTORY RESERVED EVENT RECEIVED BY PAYMENT SERVICE");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());
        System.out.println("Amount: " + event.getTotalAmount());

        paymentService.processPayment(event);

        System.out.println("INVENTORY_RESERVED EVENT PROCESSED. Event ID: " + event.getEventId());

        System.out.println("==============================");
    }
}
