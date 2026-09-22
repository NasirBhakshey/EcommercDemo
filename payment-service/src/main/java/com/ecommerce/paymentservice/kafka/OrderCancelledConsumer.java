package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.OrderCancelledEvent;
import com.ecommerce.paymentservice.service.IdempotencyService;
import com.ecommerce.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderCancelledConsumer {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public OrderCancelledConsumer(PaymentService paymentService,
                                  IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
    }

    @Transactional
    @KafkaListener(
            topics = "order-cancelled",
            groupId = "payment-order-cancelled-group",
            properties = {
                    "spring.json.value.default.type=com.ecommerce.paymentservice.event.OrderCancelledEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void consume(OrderCancelledEvent event) {

        final String consumerName = "payment-order-cancelled-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "ORDER_CANCELLED",
                consumerName
        );

        if (!claimed) {
            System.out.println("DUPLICATE ORDER_CANCELLED EVENT IGNORED. Event ID: " + event.getEventId());
            return;
        }

        System.out.println("==============================");
        System.out.println("ORDER CANCELLED EVENT RECEIVED BY PAYMENT SERVICE");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());

        paymentService.refundPayment(event.getOrderId());

        System.out.println("ORDER_CANCELLED EVENT PROCESSED. Event ID: " + event.getEventId());

        System.out.println("==============================");
    }
}
