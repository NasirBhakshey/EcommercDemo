package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.OutboxEvent;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.event.InventoryReservedEvent;
import com.ecommerce.paymentservice.event.PaymentFailedEvent;
import com.ecommerce.paymentservice.event.PaymentRefundedEvent;
import com.ecommerce.paymentservice.event.PaymentSuccessEvent;
import com.ecommerce.paymentservice.repository.OutboxEventRepository;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Payment processPayment(InventoryReservedEvent event) {

        if (paymentRepository.existsByOrderId(event.getOrderId())) {

            System.out.println("Duplicate payment event ignored. Order ID: " + event.getOrderId());

            return paymentRepository.findByOrderId(event.getOrderId()).orElseThrow();
        }

        Payment payment = new Payment();

        payment.setOrderId(event.getOrderId());
        payment.setUserId(event.getUserId());
        payment.setAmount(event.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        boolean paymentSuccessful = mockPaymentGateway();

        if (paymentSuccessful) {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(UUID.randomUUID().toString()
            );
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        payment.setUpdatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment.getStatus() == PaymentStatus.SUCCESS) {

            createPaymentSuccessOutboxEvent(savedPayment, event.getEmail());
        } else {
            createPaymentFailedOutboxEvent(savedPayment, event.getEmail());
        }

        return savedPayment;
    }

    @Transactional
    public boolean refundPayment(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Payment not found. Order ID: " + orderId
                        )
                );

        // Kafka duplicate / already refunded
        if (payment.getStatus() == PaymentStatus.REFUNDED) {

            System.out.println(
                    "Payment already refunded. Duplicate cancellation ignored. Order ID: "
                            + orderId
            );

            return false;
        }

        // Refund is only valid for a successful payment
        if (payment.getStatus() != PaymentStatus.SUCCESS) {

            System.out.println(
                    "Payment is not SUCCESS. Refund not required. Order ID: "
                            + orderId
                            + ", Status: "
                            + payment.getStatus()
            );

            return false;
        }

        // Real project:
        // Call Stripe/Razorpay/etc. refund API here.
        //
        // For your current project we simulate successful refund.

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment refundedPayment =
                paymentRepository.save(payment);

        createPaymentRefundedOutboxEvent(refundedPayment);

        System.out.println(
                "PAYMENT REFUNDED SUCCESSFULLY. Order ID: "
                        + orderId
        );

        return true;
    }

    private void createPaymentSuccessOutboxEvent(Payment payment, String email) {

        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                        UUID.randomUUID(),
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getUserId(),
                        email,
                        payment.getAmount(),
                        payment.getTransactionId(),
                        LocalDateTime.now()
                );

        try {

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setAggregateType("PAYMENT");

            outboxEvent.setAggregateId(String.valueOf(payment.getOrderId()));

            outboxEvent.setEventType("PAYMENT_SUCCESS");

            outboxEvent.setTopic("payment-success");

            outboxEvent.setPayload(objectMapper.writeValueAsString(successEvent));

            outboxEvent.setPublished(false);
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create payment success outbox event",e);
        }
    }

    private void createPaymentFailedOutboxEvent(Payment payment, String email) {

        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                        UUID.randomUUID(),
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getUserId(),
                        email,
                        payment.getAmount(),
                        "Payment Process Failed...",
                        LocalDateTime.now()
                );

        try {
            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setAggregateType("PAYMENT");

            outboxEvent.setAggregateId(String.valueOf(payment.getOrderId()));

            outboxEvent.setEventType("PAYMENT_FAILED");

            outboxEvent.setTopic("payment-failed");

            outboxEvent.setPayload(objectMapper.writeValueAsString(failedEvent));

            outboxEvent.setPublished(false);

            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new IllegalStateException("Failed to create payment failed outbox event", e);
        }
    }

    private void createPaymentRefundedOutboxEvent(
            Payment payment) {

        PaymentRefundedEvent refundedEvent =
                new PaymentRefundedEvent(
                        UUID.randomUUID(),
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getUserId(),
                        payment.getAmount(),
                        payment.getTransactionId(),
                        LocalDateTime.now()
                );

        try {

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setAggregateType("PAYMENT");

            outboxEvent.setAggregateId(
                    String.valueOf(payment.getOrderId())
            );

            outboxEvent.setEventType(
                    "PAYMENT_REFUNDED"
            );

            outboxEvent.setTopic(
                    "payment-refunded"
            );

            outboxEvent.setPayload(
                    objectMapper.writeValueAsString(refundedEvent)
            );

            outboxEvent.setPublished(false);
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to create payment refunded outbox event. Order ID: "
                            + payment.getOrderId(),
                    e
            );
        }
    }

    private boolean mockPaymentGateway() {

        return true;
    }
}