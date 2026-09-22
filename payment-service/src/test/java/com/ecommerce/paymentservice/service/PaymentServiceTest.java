package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.entity.OutboxEvent;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.event.InventoryReservedEvent;
import com.ecommerce.paymentservice.repository.OutboxEventRepository;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    void refundPayment_shouldRefundSuccessfulPaymentAndCreateOutboxEvent()
            throws Exception {

        Long orderId = 100L;

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(orderId);
        payment.setUserId(10L);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setTransactionId("TXN-100");
        payment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"payment-refunded\"}");

        boolean result = paymentService.refundPayment(orderId);

        assertTrue(result);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());

        verify(paymentRepository).findByOrderId(orderId);
        verify(paymentRepository).save(payment);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent outboxEvent = captor.getValue();

        assertEquals("PAYMENT", outboxEvent.getAggregateType());
        assertEquals(String.valueOf(orderId), outboxEvent.getAggregateId());
        assertEquals("PAYMENT_REFUNDED", outboxEvent.getEventType());
        assertEquals("payment-refunded", outboxEvent.getTopic());
        assertFalse(outboxEvent.getPublished());
        assertNotNull(outboxEvent.getCreatedAt());
    }

    @Test
    void refundPayment_shouldIgnoreAlreadyRefundedPayment() {

        Long orderId = 100L;

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(orderId);
        payment.setUserId(10L);
        payment.setStatus(PaymentStatus.REFUNDED);

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        boolean result = paymentService.refundPayment(orderId);

        assertFalse(result);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());

        verify(paymentRepository).findByOrderId(orderId);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }

    @Test
    void refundPayment_shouldNotRefundWhenPaymentIsNotSuccessful() {

        Long orderId = 100L;

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(orderId);
        payment.setUserId(10L);
        payment.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        boolean result = paymentService.refundPayment(orderId);

        assertFalse(result);
        assertEquals(PaymentStatus.FAILED, payment.getStatus());

        verify(paymentRepository).findByOrderId(orderId);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }

    @Test
    void refundPayment_shouldThrowExceptionWhenPaymentNotFound() {

        Long orderId = 999L;

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> paymentService.refundPayment(orderId)
        );

        assertEquals(
                "Payment not found. Order ID: " + orderId,
                exception.getMessage()
        );

        verify(paymentRepository).findByOrderId(orderId);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }

    @Test
    void processPayment_shouldCreateSuccessfulPaymentAndOutboxEvent()
            throws Exception {

        InventoryReservedEvent event = new InventoryReservedEvent();

        event.setOrderId(100L);
        event.setUserId(10L);
        event.setEmail("test@example.com");
        event.setTotalAmount(new BigDecimal("1500.00"));

        when(paymentRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);

                    if (payment.getId() == null) {
                        payment.setId(1L);
                    }

                    return payment;
                });

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"payment-success\"}");

        Payment result = paymentService.processPayment(event);

        assertNotNull(result);

        assertEquals(1L, result.getId());
        assertEquals(100L, result.getOrderId());
        assertEquals(10L, result.getUserId());
        assertEquals(new BigDecimal("1500.00"), result.getAmount());

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());

        assertNotNull(result.getTransactionId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(paymentRepository)
                .existsByOrderId(100L);

        verify(paymentRepository, times(2))
                .save(any(Payment.class));

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository)
                .save(captor.capture());

        OutboxEvent outboxEvent = captor.getValue();

        assertEquals("PAYMENT", outboxEvent.getAggregateType());
        assertEquals("100", outboxEvent.getAggregateId());
        assertEquals("PAYMENT_SUCCESS", outboxEvent.getEventType());
        assertEquals("payment-success", outboxEvent.getTopic());

        assertFalse(outboxEvent.getPublished());
        assertNotNull(outboxEvent.getCreatedAt());
        assertNotNull(outboxEvent.getPayload());
    }

    @Test
    void processPayment_shouldReturnExistingPaymentWhenOrderAlreadyProcessed() {

        Long orderId = 100L;

        InventoryReservedEvent event = new InventoryReservedEvent();
        event.setOrderId(orderId);
        event.setUserId(10L);
        event.setEmail("test@example.com");
        event.setTotalAmount(new BigDecimal("1500.00"));

        Payment existingPayment = new Payment();
        existingPayment.setId(1L);
        existingPayment.setOrderId(orderId);
        existingPayment.setUserId(10L);
        existingPayment.setAmount(new BigDecimal("1500.00"));
        existingPayment.setStatus(PaymentStatus.SUCCESS);
        existingPayment.setTransactionId("TXN-100");

        when(paymentRepository.existsByOrderId(orderId))
                .thenReturn(true);

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(existingPayment));

        Payment result = paymentService.processPayment(event);

        assertNotNull(result);
        assertSame(existingPayment, result);

        assertEquals(1L, result.getId());
        assertEquals(orderId, result.getOrderId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());

        verify(paymentRepository).existsByOrderId(orderId);
        verify(paymentRepository).findByOrderId(orderId);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));

        verifyNoInteractions(objectMapper);
    }

    @Test
    void processPayment_shouldThrowExceptionWhenSuccessOutboxSerializationFails()
            throws Exception {

        Long orderId = 100L;

        InventoryReservedEvent event = new InventoryReservedEvent();
        event.setOrderId(orderId);
        event.setUserId(10L);
        event.setEmail("test@example.com");
        event.setTotalAmount(new BigDecimal("1500.00"));

        when(paymentRepository.existsByOrderId(orderId))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);

                    if (payment.getId() == null) {
                        payment.setId(1L);
                    }

                    return payment;
                });

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> paymentService.processPayment(event)
        );

        assertEquals(
                "Failed to create payment success outbox event",
                exception.getMessage()
        );

        assertNotNull(exception.getCause());

        verify(paymentRepository)
                .existsByOrderId(orderId);

        verify(paymentRepository, times(2))
                .save(any(Payment.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }
}
