package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.PaymentFailedEvent;
import com.ecommerce.productservice.service.IdempotencyService;
import com.ecommerce.productservice.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentFailedConsumer {

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;

    public PaymentFailedConsumer(InventoryService inventoryService,
                                 IdempotencyService idempotencyService){
        this.idempotencyService = idempotencyService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-failed",
            groupId = "product-payment-failed-group",
            properties = {
                    "spring.json.value.default.type=com.ecommerce.productservice.event.PaymentFailedEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void consume(PaymentFailedEvent event) {


        final String consumerName = "product-payment-failed-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_FAILED",
                consumerName
        );

        if (!claimed) {
            System.out.println(
                    "DUPLICATE PAYMENT_FAILED EVENT IGNORED. Event ID: "
                            + event.getEventId()
            );
            return;
        }

        System.out.println("==============================");
        System.out.println("PAYMENT FAILED RECEIVED BY PRODUCT SERVICE");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());

        inventoryService.restoreStock(event.getOrderId());

        System.out.println("PAYMENT_FAILED EVENT PROCESSED. Event ID: " + event.getEventId());

        System.out.println("==============================");
    }
}
