package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.PaymentFailedEvent;
import com.ecommerce.orderservice.event.PaymentSuccessEvent;
import com.ecommerce.orderservice.service.IdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentEventConsumer {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public PaymentEventConsumer(OrderService orderService,
                                IdempotencyService idempotencyService){
        this.idempotencyService = idempotencyService;
        this.orderService = orderService;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-success",
            groupId = "order-payment-success-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.orderservice.event.PaymentSuccessEvent"
            }
    )
    public void consumeSuccess(PaymentSuccessEvent event){

        final String consumerName = "order-payment-success-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_SUCCESS",
                consumerName
        );

        if (!claimed) {
            System.out.println(
                    "Duplicate PAYMENT_SUCCESS ignored. eventId="
                            + event.getEventId()
            );
            return;
        }

        System.out.println(
                "Processing PAYMENT_SUCCESS. orderId="
                        + event.getOrderId()
                        + ", eventId="
                        + event.getEventId()
        );

        System.out.println("==============================");
        System.out.println("PAYMENT SUCCESS RECEIVED");
        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());
        System.out.println("Payment ID: " + event.getPaymentId());

        orderService.markPaymentSuccess(event.getOrderId());

        System.out.println("ORDER UPDATED AFTER PAYMENT SUCCESS");
        System.out.println("==============================");

    }


    @Transactional
    @KafkaListener(
            topics = "payment-failed",
            groupId = "order-payment-failed-group",
            properties = {
                    "spring.json.use.type.headers=false",
                    "spring.json.value.default.type=com.ecommerce.orderservice.event.PaymentFailedEvent"
            }
    )
    public void consumeFailure(PaymentFailedEvent event) {

        final String consumerName = "order-payment-failed-group";

        boolean claimed = idempotencyService.claimEvent(
                event.getEventId(),
                "PAYMENT_FAILED",
                consumerName
        );

        if (!claimed) {
            System.out.println(
                    "Duplicate PAYMENT_FAILED ignored. eventId="
                            + event.getEventId()
            );
            return;
        }

        System.out.println(
                "Processing PAYMENT_FAILED. orderId="
                        + event.getOrderId()
                        + ", eventId="
                        + event.getEventId()
        );

        orderService.markPaymentFailed(event.getOrderId());

        System.out.println(
                "ORDER UPDATED AFTER PAYMENT FAILURE. Order ID: "
                        + event.getOrderId()
        );
        System.out.println("==============================");
    }
}
