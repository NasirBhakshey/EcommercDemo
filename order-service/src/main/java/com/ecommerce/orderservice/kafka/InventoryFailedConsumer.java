package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.InventoryFailedEvent;
import com.ecommerce.orderservice.service.IdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryFailedConsumer {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public InventoryFailedConsumer(OrderService orderService,
                                   IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.orderService = orderService;
    }

    @Transactional
    @KafkaListener(
            topics = "inventory-failed",
            groupId = "order-inventory-failed-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.orderservice.event.InventoryFailedEvent"
            }
    )
    public void consume(InventoryFailedEvent event) {

        final String consumerName = "order-inventory-failed-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "INVENTORY_FAILED",
                consumerName
        );

        if (!claimed) {
            System.out.println("DUPLICATE INVENTORY_FAILED EVENT IGNORED. Event ID: " + event.getEventId());
            return;
        }

        System.out.println("INVENTORY FAILED EVENT RECEIVED. Order ID: " + event.getOrderId());

        System.out.println("Inventory failure reason: " + event.getReason());

        orderService.markInventoryFailed(event.getOrderId());
    }
}
