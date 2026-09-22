package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.OrderCreatedEvent;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.service.IdempotencyService;
import com.ecommerce.productservice.service.InventoryFailureService;
import com.ecommerce.productservice.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryFailureService inventoryFailureService;
    private final IdempotencyService idempotencyService;

    public OrderEventConsumer(InventoryService inventoryService,
                              InventoryFailureService inventoryFailureService,
                              IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.inventoryService = inventoryService;
        this.inventoryFailureService = inventoryFailureService;
    }

    @Transactional
    @KafkaListener(
            topics = "order-created",
            groupId = "product-inventory-group",
            properties = {
                    "spring.json.value.default.type=com.ecommerce.productservice.event.OrderCreatedEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void consume(OrderCreatedEvent event) {

        final String consumerName = "product-inventory-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "ORDER_CREATED",
                consumerName
        );

        if (!claimed) {
            System.out.println("DUPLICATE ORDER_CREATED EVENT IGNORED. Event ID: " + event.getEventId());
            return;
        }

        System.out.println("==============================");
        System.out.println("ORDER CREATED RECEIVED BY PRODUCT SERVICE");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());


        try {

            boolean reserved = inventoryService.reduceStock(event);

            if (!reserved) {
                System.out.println(
                        "INVENTORY EVENT ALREADY PROCESSED. Order ID: "
                                + event.getOrderId()
                );
                return;
            }

        } catch (InsufficientStockException e) {

            System.out.println(
                    "INVENTORY RESERVATION FAILED. Order ID: "
                            + event.getOrderId()
                            + ", Reason: "
                            + e.getMessage()
            );

            inventoryFailureService.createFailureEvent(
                    event,
                    e.getMessage()
            );

            return;
        }

        // KEEP YOUR EXISTING inventory reservation call here.
        // Do not replace it with guessed code.

        System.out.println("ORDER_CREATED EVENT PROCESSED. Event ID: " + event.getEventId());

        System.out.println("==============================");
    }

}
