package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.PaymentRefundedEvent;
import com.ecommerce.orderservice.service.IdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentRefundedConsumer {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public PaymentRefundedConsumer(OrderService orderService,
                                   IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        this.orderService = orderService;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-refunded",
            groupId = "order-payment-refunded-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.orderservice.event.PaymentRefundedEvent"
            }
    )
    public void consume(PaymentRefundedEvent event) {

        final String consumerName = "order-payment-refunded-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_REFUNDED",
                consumerName
        );

        if (!claimed) {
            System.out.println(
                    "Duplicate PAYMENT_REFUNDED ignored. eventId="
                            + event.getEventId()
            );
            return;
        }

        System.out.println(
                "Processing PAYMENT_REFUNDED. orderId="
                        + event.getOrderId()
                        + ", eventId="
                        + event.getEventId()
        );

        orderService.markPaymentRefunded(event.getOrderId());
    }
}
