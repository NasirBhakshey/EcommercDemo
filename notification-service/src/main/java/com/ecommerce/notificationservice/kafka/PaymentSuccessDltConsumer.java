package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.events.PaymentSuccessEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentSuccessDltConsumer {

    @KafkaListener(
            topics = "payment-success.DLT",
            groupId = "notification-payment-success-dlt-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.notificationservice.events.PaymentSuccessEvent"
            }
    )
    public void consume(PaymentSuccessEvent event) {

        System.err.println("PAYMENT SUCCESS EVENT MOVED TO DLT");
        System.err.println("Order ID: " + event.getOrderId());
        System.err.println("Email: " + event.getEmail());
    }
}
