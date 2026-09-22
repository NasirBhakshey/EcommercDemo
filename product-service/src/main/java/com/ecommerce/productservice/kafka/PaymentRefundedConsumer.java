package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.PaymentRefundedEvent;
import com.ecommerce.productservice.service.IdempotencyService;
import com.ecommerce.productservice.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentRefundedConsumer {

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;

    public PaymentRefundedConsumer(InventoryService inventoryService,
            IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-refunded",
            groupId = "product-payment-refunded-group",
            properties = {
                    "spring.json.value.default.type=com.ecommerce.productservice.event.PaymentRefundedEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void consume(PaymentRefundedEvent event) {

        final String consumerName =
                "product-payment-refunded-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_REFUNDED",
                consumerName
        );

        if (!claimed) {
            System.out.println(
                    "DUPLICATE PAYMENT_REFUNDED EVENT IGNORED. Event ID: "
                            + event.getEventId()
            );
            return;
        }

        System.out.println("==============================");
        System.out.println("PAYMENT REFUNDED RECEIVED BY PRODUCT SERVICE");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());

        inventoryService.restoreStockAfterOrderCancellation(event.getOrderId());

        System.out.println("PAYMENT_REFUNDED EVENT PROCESSED. Event ID: " + event.getEventId());

        System.out.println("==============================");
    }
}
