package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.entity.OutboxEvent;
import com.ecommerce.paymentservice.event.PaymentSuccessEvent;
import com.ecommerce.paymentservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(
                outboxEventRepository,
                kafkaTemplate,
                objectMapper
        );
    }

    @Test
    void publishPendingEvents_shouldPublishPaymentSuccessEvent()
            throws Exception {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setAggregateType("PAYMENT");
        outboxEvent.setAggregateId("100");
        outboxEvent.setEventType("PAYMENT_SUCCESS");
        outboxEvent.setTopic("payment-success");
        outboxEvent.setPayload("{\"event\":\"payment-success\"}");
        outboxEvent.setPublished(false);

        PaymentSuccessEvent paymentSuccessEvent = new PaymentSuccessEvent();

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                PaymentSuccessEvent.class
        )).thenReturn(paymentSuccessEvent);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                "payment-success",
                "100",
                paymentSuccessEvent
        )).thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate).send(
                "payment-success",
                "100",
                paymentSuccessEvent
        );

        verify(outboxEventRepository).save(outboxEvent);

        assertTrue(outboxEvent.getPublished());
        assertNotNull(outboxEvent.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldKeepEventUnpublishedWhenKafkaSendFails()
            throws Exception {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setAggregateType("PAYMENT");
        outboxEvent.setAggregateId("100");
        outboxEvent.setEventType("PAYMENT_SUCCESS");
        outboxEvent.setTopic("payment-success");
        outboxEvent.setPayload("{\"event\":\"payment-success\"}");
        outboxEvent.setPublished(false);

        PaymentSuccessEvent paymentSuccessEvent =
                new PaymentSuccessEvent();

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                PaymentSuccessEvent.class
        )).thenReturn(paymentSuccessEvent);

        CompletableFuture<SendResult<String, Object>> failedFuture =
                new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException("Kafka unavailable")
        );

        when(kafkaTemplate.send(
                "payment-success",
                "100",
                paymentSuccessEvent
        )).thenReturn(failedFuture);

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate).send(
                "payment-success",
                "100",
                paymentSuccessEvent
        );

        // Failed publishing must NOT update the database record.
        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));

        assertFalse(outboxEvent.getPublished());
        assertNull(outboxEvent.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldIgnoreUnknownEventType() {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setAggregateType("PAYMENT");
        outboxEvent.setAggregateId("100");
        outboxEvent.setEventType("UNKNOWN_EVENT");
        outboxEvent.setTopic("unknown-topic");
        outboxEvent.setPayload("{}");
        outboxEvent.setPublished(false);

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        outboxPublisher.publishPendingEvents();

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(kafkaTemplate);

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));

        assertFalse(outboxEvent.getPublished());
        assertNull(outboxEvent.getPublishedAt());
    }

}
