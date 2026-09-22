package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.PaymentSuccessEvent;
import com.ecommerce.productservice.service.IdempotencyService;
import com.ecommerce.productservice.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentSuccessConsumer {

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;

    public PaymentSuccessConsumer(InventoryService inventoryService,
                                  IdempotencyService idempotencyService){
        this.idempotencyService = idempotencyService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-success",
            groupId = "product-payment-success-group",
            properties = {
                    "spring.json.value.default.type=com.ecommerce.productservice.event.PaymentSuccessEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void consume(PaymentSuccessEvent event) {

        final String consumerName = "product-payment-success-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_SUCCESS",
                consumerName
        );

        if (!claimed) {
            System.out.println(
                    "DUPLICATE PAYMENT_SUCCESS EVENT IGNORED. Event ID: "
                            + event.getEventId()
            );
            return;
        }

        System.out.println("==============================");
        System.out.println("PAYMENT SUCCESS RECEIVED BY PRODUCT SERVICE");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());

        inventoryService.commitStock(event.getOrderId());
        System.out.println("PAYMENT_SUCCESS EVENT PROCESSED. Event ID: " + event.getEventId());

        System.out.println("==============================");
    }
}
