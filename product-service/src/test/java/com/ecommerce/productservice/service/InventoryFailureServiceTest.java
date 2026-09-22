package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.event.OrderCreatedEvent;
import com.ecommerce.productservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryFailureServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private InventoryFailureService inventoryFailureService;

    @BeforeEach
    void setUp() {
        inventoryFailureService = new InventoryFailureService(
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    void createFailureEvent_shouldCreateInventoryFailedOutboxEvent()
            throws Exception {

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(100L);
        event.setUserId(10L);
        event.setEmail("test@example.com");

        String reason =
                "Insufficient stock or product not found. Product ID: 1";

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"inventory-failed\"}");

        inventoryFailureService.createFailureEvent(event, reason);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository)
                .save(captor.capture());

        OutboxEvent outbox = captor.getValue();

        assertEquals("INVENTORY", outbox.getAggregateType());
        assertEquals("100", outbox.getAggregateId());
        assertEquals("INVENTORY_FAILED", outbox.getEventType());
        assertEquals("inventory-failed", outbox.getTopic());

        assertEquals(
                "{\"event\":\"inventory-failed\"}",
                outbox.getPayload()
        );

        assertFalse(outbox.isPublished());
        assertNotNull(outbox.getCreatedAt());

        verify(objectMapper)
                .writeValueAsString(any());
    }

    @Test
    void createFailureEvent_shouldThrowExceptionWhenSerializationFails()
            throws Exception {

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(100L);
        event.setUserId(10L);
        event.setEmail("test@example.com");

        String reason =
                "Insufficient stock or product not found. Product ID: 1";

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> inventoryFailureService.createFailureEvent(
                                event,
                                reason
                        )
                );

        assertEquals(
                "Failed to create inventory failure outbox event",
                exception.getMessage()
        );

        assertNotNull(exception.getCause());

        verify(objectMapper)
                .writeValueAsString(any());

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}
