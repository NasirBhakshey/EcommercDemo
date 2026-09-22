package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.InventoryFailedEvent;
import com.ecommerce.orderservice.service.IdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryFailedConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private IdempotencyService idempotencyService;

    private InventoryFailedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryFailedConsumer(
                orderService,
                idempotencyService
        );
    }

    @Test
    void consume_shouldMarkInventoryFailed_whenEventIsNew() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        InventoryFailedEvent event = new InventoryFailedEvent(
                eventId,
                orderId,
                10L,
                "test@example.com",
                "Insufficient stock",
                LocalDateTime.now()
        );

        when(idempotencyService.claimEvent(
                eventId,
                "INVENTORY_FAILED",
                "order-inventory-failed-group"
        )).thenReturn(true);

        // Act
        consumer.consume(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "INVENTORY_FAILED",
                        "order-inventory-failed-group"
                );

        verify(orderService, times(1))
                .markInventoryFailed(orderId);
    }

    @Test
    void consume_shouldIgnoreEvent_whenEventIsDuplicate() {

        // Arrange
        UUID eventId = UUID.randomUUID();
        Long orderId = 100L;

        InventoryFailedEvent event = new InventoryFailedEvent(
                eventId,
                orderId,
                10L,
                "test@example.com",
                "Insufficient stock",
                LocalDateTime.now()
        );

        when(idempotencyService.claimEvent(
                eventId,
                "INVENTORY_FAILED",
                "order-inventory-failed-group"
        )).thenReturn(false);

        // Act
        consumer.consume(event);

        // Assert
        verify(idempotencyService, times(1))
                .claimEvent(
                        eventId,
                        "INVENTORY_FAILED",
                        "order-inventory-failed-group"
                );

        verify(orderService, never())
                .markInventoryFailed(anyLong());

        verifyNoMoreInteractions(orderService);
    }

}
