package com.ecommerce.productservice.service;


import com.ecommerce.productservice.entity.InventoryReservation;
import com.ecommerce.productservice.entity.InventoryReservationStatus;
import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.event.OrderCreatedEvent;
import com.ecommerce.productservice.event.OrderItemEvent;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.repository.InventoryReservationRepository;
import com.ecommerce.productservice.repository.OutboxEventRepository;
import com.ecommerce.productservice.repository.ProcessedInventoryEventRepository;
import com.ecommerce.productservice.repository.productRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private productRepository productRepository;

    @Mock
    private ProcessedInventoryEventRepository processedEventRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                productRepository,
                processedEventRepository,
                inventoryReservationRepository,
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    void reduceStock_shouldReserveInventoryAndCreateOutboxEvent() throws Exception {

        OrderItemEvent item = new OrderItemEvent();
        item.setProductId(1L);
        item.setQuantity(2);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(100L);
        event.setUserId(10L);
        event.setEmail("test@example.com");
        event.setTotalAmount(new BigDecimal("1500.00"));
        event.setItems(List.of(item));

        when(processedEventRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(productRepository.reduceStock(1L, 2))
                .thenReturn(1);

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"inventory-reserved\"}");

        boolean result = inventoryService.reduceStock(event);

        assertTrue(result);

        verify(productRepository).reduceStock(1L, 2);

        ArgumentCaptor<InventoryReservation> reservationCaptor =
                ArgumentCaptor.forClass(InventoryReservation.class);

        verify(inventoryReservationRepository)
                .save(reservationCaptor.capture());

        InventoryReservation reservation = reservationCaptor.getValue();

        assertEquals(100L, reservation.getOrderId());
        assertEquals(1L, reservation.getProductId());
        assertEquals(2, reservation.getQuantity());
        assertEquals(
                InventoryReservationStatus.RESERVED,
                reservation.getStatus()
        );

        ArgumentCaptor<OutboxEvent> outboxCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository)
                .save(outboxCaptor.capture());

        OutboxEvent outbox = outboxCaptor.getValue();

        assertEquals("ORDER", outbox.getAggregateType());
        assertEquals("100", outbox.getAggregateId());
        assertEquals("INVENTORY_RESERVED", outbox.getEventType());
        assertEquals("inventory-reserved", outbox.getTopic());
        assertFalse(outbox.isPublished());
        assertNotNull(outbox.getCreatedAt());
        assertNotNull(outbox.getPayload());
    }

    @Test
    void reduceStock_shouldIgnoreDuplicateOrderEvent() {

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(100L);

        when(processedEventRepository.existsByOrderId(100L))
                .thenReturn(true);

        boolean result = inventoryService.reduceStock(event);

        assertFalse(result);

        verify(processedEventRepository)
                .existsByOrderId(100L);

        verifyNoInteractions(productRepository);
        verifyNoInteractions(inventoryReservationRepository);
        verifyNoInteractions(outboxEventRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void reduceStock_shouldThrowExceptionWhenOrderContainsNoItems() {

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(100L);
        event.setItems(List.of());

        when(processedEventRepository.existsByOrderId(100L))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.reduceStock(event)
        );

        assertEquals(
                "Order Contains No Items. Order ID: 100",
                exception.getMessage()
        );

        verify(processedEventRepository)
                .existsByOrderId(100L);

        verifyNoInteractions(productRepository);
        verifyNoInteractions(inventoryReservationRepository);
        verifyNoInteractions(outboxEventRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void reduceStock_shouldThrowExceptionWhenStockIsInsufficient() {

        OrderItemEvent item = new OrderItemEvent();
        item.setProductId(1L);
        item.setQuantity(5);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(100L);
        event.setItems(List.of(item));

        when(processedEventRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(productRepository.reduceStock(1L, 5))
                .thenReturn(0);

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.reduceStock(event)
        );

        assertEquals(
                "Insufficient stock or product not found. Product ID: 1",
                exception.getMessage()
        );

        verify(productRepository)
                .reduceStock(1L, 5);

        verifyNoInteractions(inventoryReservationRepository);
        verifyNoInteractions(outboxEventRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void restoreStock_shouldRestoreReservedInventoryAfterPaymentFailure() {

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(100L);
        reservation.setProductId(1L);
        reservation.setQuantity(2);
        reservation.setStatus(InventoryReservationStatus.RESERVED);

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.RESERVED
        )).thenReturn(List.of(reservation));

        when(productRepository.restoreStock(1L, 2))
                .thenReturn(1);

        boolean result = inventoryService.restoreStock(100L);

        assertTrue(result);

        verify(productRepository)
                .restoreStock(1L, 2);

        assertEquals(
                InventoryReservationStatus.RESTORED,
                reservation.getStatus()
        );

        verify(inventoryReservationRepository)
                .saveAll(anyList());
    }

    @Test
    void restoreStock_shouldReturnFalseWhenNoReservedInventoryExists() {

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.RESERVED
        )).thenReturn(List.of());

        boolean result = inventoryService.restoreStock(100L);

        assertFalse(result);

        verify(inventoryReservationRepository)
                .findByOrderIdAndStatus(
                        100L,
                        InventoryReservationStatus.RESERVED
                );

        verifyNoInteractions(productRepository);

        verify(inventoryReservationRepository, never())
                .saveAll(anyList());
    }

    @Test
    void restoreStock_shouldThrowExceptionWhenProductCannotBeRestored() {

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(100L);
        reservation.setProductId(1L);
        reservation.setQuantity(2);
        reservation.setStatus(InventoryReservationStatus.RESERVED);

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.RESERVED
        )).thenReturn(List.of(reservation));

        when(productRepository.restoreStock(1L, 2))
                .thenReturn(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> inventoryService.restoreStock(100L)
        );

        assertEquals(
                "Product not found while restoring stock. Product ID: 1",
                exception.getMessage()
        );

        verify(productRepository)
                .restoreStock(1L, 2);

        // Status must remain RESERVED because restoration failed.
        assertEquals(
                InventoryReservationStatus.RESERVED,
                reservation.getStatus()
        );

        verify(inventoryReservationRepository, never())
                .saveAll(anyList());
    }

    @Test
    void commitStock_shouldCommitReservedInventoryAfterPaymentSuccess() {

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(100L);
        reservation.setProductId(1L);
        reservation.setQuantity(2);
        reservation.setStatus(InventoryReservationStatus.RESERVED);

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.RESERVED
        )).thenReturn(List.of(reservation));

        boolean result = inventoryService.commitStock(100L);

        assertTrue(result);

        assertEquals(
                InventoryReservationStatus.COMMITTED,
                reservation.getStatus()
        );

        verify(inventoryReservationRepository)
                .findByOrderIdAndStatus(
                        100L,
                        InventoryReservationStatus.RESERVED
                );

        verify(inventoryReservationRepository)
                .saveAll(anyList());

        // Committing does NOT add stock back.
        verifyNoInteractions(productRepository);
    }

    @Test
    void commitStock_shouldReturnFalseWhenNoReservedInventoryExists() {

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.RESERVED
        )).thenReturn(List.of());

        boolean result = inventoryService.commitStock(100L);

        assertFalse(result);

        verify(inventoryReservationRepository)
                .findByOrderIdAndStatus(
                        100L,
                        InventoryReservationStatus.RESERVED
                );

        verify(inventoryReservationRepository, never())
                .saveAll(anyList());

        verifyNoInteractions(productRepository);
    }

    @Test
    void restoreStockAfterOrderCancellation_shouldRestoreCommittedInventory() {

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(100L);
        reservation.setProductId(1L);
        reservation.setQuantity(2);
        reservation.setStatus(InventoryReservationStatus.COMMITTED);

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.COMMITTED
        )).thenReturn(List.of(reservation));

        when(productRepository.restoreStock(1L, 2))
                .thenReturn(1);

        boolean result =
                inventoryService.restoreStockAfterOrderCancellation(100L);

        assertTrue(result);

        verify(productRepository)
                .restoreStock(1L, 2);

        assertEquals(
                InventoryReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(inventoryReservationRepository)
                .saveAll(anyList());
    }

    @Test
    void restoreStockAfterOrderCancellation_shouldReturnFalseWhenNoCommittedInventoryExists() {

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.COMMITTED
        )).thenReturn(List.of());

        boolean result =
                inventoryService.restoreStockAfterOrderCancellation(100L);

        assertFalse(result);

        verify(inventoryReservationRepository)
                .findByOrderIdAndStatus(
                        100L,
                        InventoryReservationStatus.COMMITTED
                );

        verifyNoInteractions(productRepository);

        verify(inventoryReservationRepository, never())
                .saveAll(anyList());
    }

    @Test
    void restoreStockAfterOrderCancellation_shouldThrowExceptionWhenProductCannotBeRestored() {

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(100L);
        reservation.setProductId(1L);
        reservation.setQuantity(2);
        reservation.setStatus(InventoryReservationStatus.COMMITTED);

        when(inventoryReservationRepository.findByOrderIdAndStatus(
                100L,
                InventoryReservationStatus.COMMITTED
        )).thenReturn(List.of(reservation));

        when(productRepository.restoreStock(1L, 2))
                .thenReturn(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> inventoryService.restoreStockAfterOrderCancellation(100L)
        );

        assertEquals(
                "Product not found while restoring cancelled order stock. Product ID: 1",
                exception.getMessage()
        );

        verify(productRepository)
                .restoreStock(1L, 2);

        // Restoration failed, so status must remain COMMITTED.
        assertEquals(
                InventoryReservationStatus.COMMITTED,
                reservation.getStatus()
        );

        verify(inventoryReservationRepository, never())
                .saveAll(anyList());
    }

    @Test
    void reduceStock_shouldThrowExceptionWhenOutboxSerializationFails() throws Exception {

        OrderItemEvent item = new OrderItemEvent();
        item.setProductId(1L);
        item.setQuantity(2);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(100L);
        event.setUserId(10L);
        event.setEmail("test@example.com");
        event.setTotalAmount(new BigDecimal("1500.00"));
        event.setItems(List.of(item));

        when(processedEventRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(productRepository.reduceStock(1L, 2))
                .thenReturn(1);

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failed") {});

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> inventoryService.reduceStock(event)
        );

        assertEquals(
                "Failed to serialize InventoryReservedEvent. Order ID: 100",
                exception.getMessage()
        );

        verify(productRepository)
                .reduceStock(1L, 2);

        verify(inventoryReservationRepository)
                .save(any(InventoryReservation.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }
}
