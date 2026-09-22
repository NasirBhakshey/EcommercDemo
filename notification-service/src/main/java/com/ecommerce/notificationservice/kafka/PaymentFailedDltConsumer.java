package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentFailedDltConsumer {

    @KafkaListener(
            topics = "payment-failed.DLT",
            groupId = "notification-payment-failed-dlt-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.notificationservice.events.PaymentFailedEvent"
            }
    )
    public void consume(PaymentFailedEvent event) {

        System.err.println("PAYMENT FAILED EVENT MOVED TO DLT");
        System.err.println("Order ID: " + event.getOrderId());
        System.err.println("Email: " + event.getEmail());
        System.err.println("Reason: " + event.getReason());
    }
}
