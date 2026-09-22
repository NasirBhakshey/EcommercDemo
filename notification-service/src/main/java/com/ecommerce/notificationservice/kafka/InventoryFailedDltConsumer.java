package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.InventoryFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryFailedDltConsumer {

    @KafkaListener(
            topics = "inventory-failed.DLT",
            groupId = "notification-inventory-failed-dlt-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.notificationservice.events.InventoryFailedEvent"
            }
    )
    public void consume(InventoryFailedEvent event) {

        System.err.println("INVENTORY FAILED EVENT MOVED TO DLT");
        System.err.println("Order ID: " + event.getOrderId());
        System.err.println("Email: " + event.getEmail());
        System.err.println("Reason: " + event.getReason());
    }
}
