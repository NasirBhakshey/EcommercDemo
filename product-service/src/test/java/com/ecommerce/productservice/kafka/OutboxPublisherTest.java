package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.event.InventoryFailedEvent;
import com.ecommerce.productservice.event.InventoryReservedEvent;
import com.ecommerce.productservice.repository.OutboxEventRepository;
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
import static org.mockito.ArgumentMatchers.any;
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
    void publishPendingEvents_shouldPublishInventoryReservedEventSuccessfully()
            throws Exception {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId("100");
        outboxEvent.setEventType("INVENTORY_RESERVED");
        outboxEvent.setTopic("inventory-reserved");
        outboxEvent.setPayload("{\"orderId\":100}");
        outboxEvent.setPublished(false);

        InventoryReservedEvent inventoryEvent =
                new InventoryReservedEvent();

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                InventoryReservedEvent.class
        )).thenReturn(inventoryEvent);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                "inventory-reserved",
                "100",
                inventoryEvent
        )).thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate).send(
                "inventory-reserved",
                "100",
                inventoryEvent
        );

        verify(outboxEventRepository)
                .save(outboxEvent);

        assertTrue(outboxEvent.isPublished());
        assertNotNull(outboxEvent.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldKeepEventUnpublishedWhenKafkaSendFails()
            throws Exception {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId("100");
        outboxEvent.setEventType("INVENTORY_RESERVED");
        outboxEvent.setTopic("inventory-reserved");
        outboxEvent.setPayload("{\"orderId\":100}");
        outboxEvent.setPublished(false);

        InventoryReservedEvent inventoryEvent =
                new InventoryReservedEvent();

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                InventoryReservedEvent.class
        )).thenReturn(inventoryEvent);

        CompletableFuture<SendResult<String, Object>> failedFuture =
                new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException("Kafka unavailable")
        );

        when(kafkaTemplate.send(
                "inventory-reserved",
                "100",
                inventoryEvent
        )).thenReturn(failedFuture);

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate).send(
                "inventory-reserved",
                "100",
                inventoryEvent
        );

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));

        assertFalse(outboxEvent.isPublished());
        assertNull(outboxEvent.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldSkipUnsupportedEventType() {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId("100");
        outboxEvent.setEventType("UNKNOWN_EVENT");
        outboxEvent.setTopic("unknown-topic");
        outboxEvent.setPayload("{}");
        outboxEvent.setPublished(false);

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        outboxPublisher.publishPendingEvents();

        verifyNoInteractions(kafkaTemplate);
        verifyNoInteractions(objectMapper);

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));

        assertFalse(outboxEvent.isPublished());
        assertNull(outboxEvent.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldPublishInventoryFailedEventSuccessfully()
            throws Exception {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(2L);
        outboxEvent.setAggregateType("INVENTORY");
        outboxEvent.setAggregateId("100");
        outboxEvent.setEventType("INVENTORY_FAILED");
        outboxEvent.setTopic("inventory-failed");
        outboxEvent.setPayload("{\"orderId\":100}");
        outboxEvent.setPublished(false);

        InventoryFailedEvent inventoryFailedEvent =
                new InventoryFailedEvent();

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                InventoryFailedEvent.class
        )).thenReturn(inventoryFailedEvent);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                "inventory-failed",
                "100",
                inventoryFailedEvent
        )).thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(objectMapper).readValue(
                outboxEvent.getPayload(),
                InventoryFailedEvent.class
        );

        verify(kafkaTemplate).send(
                "inventory-failed",
                "100",
                inventoryFailedEvent
        );

        verify(outboxEventRepository)
                .save(outboxEvent);

        assertTrue(outboxEvent.isPublished());
        assertNotNull(outboxEvent.getPublishedAt());
    }
}
