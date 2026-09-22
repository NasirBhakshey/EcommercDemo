package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.event.OrderCreatedEvent;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.service.IdempotencyService;
import com.ecommerce.productservice.service.InventoryFailureService;
import com.ecommerce.productservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryFailureService inventoryFailureService;

    @Mock
    private IdempotencyService idempotencyService;

    private OrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(
                inventoryService,
                inventoryFailureService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldProcessNewOrderCreatedEvent() {

        UUID eventId = UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        )).thenReturn(true);

        when(inventoryService.reduceStock(event))
                .thenReturn(true);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        );

        verify(inventoryService)
                .reduceStock(event);

        verifyNoInteractions(inventoryFailureService);
    }

    @Test
    void consume_shouldIgnoreDuplicateOrderCreatedEvent() {

        UUID eventId = UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        )).thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        );

        verifyNoInteractions(inventoryService);
        verifyNoInteractions(inventoryFailureService);
    }

    @Test
    void consume_shouldStopWhenInventoryWasAlreadyProcessed() {

        UUID eventId = UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        when(idempotencyService.claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        )).thenReturn(true);

        when(inventoryService.reduceStock(event))
                .thenReturn(false);

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        );

        verify(inventoryService)
                .reduceStock(event);

        verifyNoInteractions(inventoryFailureService);
    }

    @Test
    void consume_shouldCreateInventoryFailureEventWhenStockIsInsufficient() {

        UUID eventId = UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(eventId);
        event.setOrderId(100L);

        String reason =
                "Insufficient stock or product not found. Product ID: 1";

        when(idempotencyService.claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        )).thenReturn(true);

        when(inventoryService.reduceStock(event))
                .thenThrow(new InsufficientStockException(reason));

        consumer.consume(event);

        verify(idempotencyService).claimEvent(
                eventId,
                "ORDER_CREATED",
                "product-inventory-group"
        );

        verify(inventoryService)
                .reduceStock(event);

        verify(inventoryFailureService)
                .createFailureEvent(event, reason);
    }
}
