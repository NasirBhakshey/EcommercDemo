package com.ecommerce.orderservice.service.Impl;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.ProductResponse;
import com.ecommerce.orderservice.dto.orderItemRequest;
import com.ecommerce.orderservice.dto.orderRequest;
import com.ecommerce.orderservice.dto.orderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.exception.ForbiddenOrderAccessException;
import com.ecommerce.orderservice.exception.InvalidOrderStateException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OutboxEventRepository;
import com.ecommerce.orderservice.repository.orderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private orderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private orderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new orderServiceImpl(
                orderRepository,
                productClient,
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    void createOrder_shouldCalculateTotalAndCreateOutboxEvent() throws Exception {

        // Arrange
        Long userId = 10L;
        String email = "test@example.com";

        orderItemRequest itemRequest = new orderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        orderRequest request = new orderRequest();
        request.setItems(List.of(itemRequest));

        ProductResponse product = new ProductResponse(
                1L,
                "Laptop",
                new BigDecimal("10000.00"),
                10
        );

        when(productClient.getProductById(1L))
                .thenReturn(product);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);

                    order.setId(100L);
                    order.setCreatedAt(LocalDateTime.now());

                    return order;
                });

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"order-created\"}");

        // Act
        orderResponse response =
                orderService.createOrder(userId, email, request);

        // Assert
        assertNotNull(response);

        assertEquals(100L, response.getId());
        assertEquals(userId, response.getUserId());

        assertEquals(
                0,
                new BigDecimal("20000.00")
                        .compareTo(response.getTotalAmount())
        );

        verify(productClient, times(1))
                .getProductById(1L);

        verify(orderRepository, times(1))
                .save(any(Order.class));

        ArgumentCaptor<OutboxEvent> outboxCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository, times(1))
                .save(outboxCaptor.capture());

        OutboxEvent outboxEvent = outboxCaptor.getValue();

        assertEquals("ORDER", outboxEvent.getAggregateType());
        assertEquals("100", outboxEvent.getAggregateId());
        assertEquals("ORDER_CREATED", outboxEvent.getEventType());
        assertEquals("order-created", outboxEvent.getTopic());

        assertFalse(outboxEvent.isPublished());

        assertEquals(
                "{\"event\":\"order-created\"}",
                outboxEvent.getPayload()
        );

        assertNotNull(outboxEvent.getCreatedAt());
    }

    @Test
    void getOrderById_shouldThrowException_whenUserDoesNotOwnOrder() {

        // Arrange
        Long orderId = 100L;
        Long orderOwnerId = 10L;
        Long requestingUserId = 20L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(orderOwnerId);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act + Assert
        ForbiddenOrderAccessException exception =
                assertThrows(
                        ForbiddenOrderAccessException.class,
                        () -> orderService.getOrderById(
                                orderId,
                                requestingUserId,
                                false
                        )
                );

        assertEquals(
                "You are not allowed to access this order",
                exception.getMessage()
        );

        verify(orderRepository, times(1))
                .findById(orderId);
    }

    @Test
    void cancelOrder_shouldThrowException_whenOrderIsDelivered() {

        // Arrange
        Long orderId = 100L;
        Long userId = 10L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act + Assert
        ForbiddenOrderAccessException exception =
                assertThrows(
                        ForbiddenOrderAccessException.class,
                        () -> orderService.cancelOrder(
                                orderId,
                                userId,
                                false
                        )
                );

        assertEquals(
                "Delivered Order Cannot be Cancelled...",
                exception.getMessage()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, never())
                .save(any(Order.class));

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));
    }

    @Test
    void cancelOrder_shouldCancelOrderAndCreateOutboxEvent() throws Exception {

        // Arrange
        Long orderId = 100L;
        Long userId = 10L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PAYMENT_SUCCESS);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"order-cancelled\"}");

        // Act
        orderService.cancelOrder(
                orderId,
                userId,
                false
        );

        // Assert
        assertEquals(
                OrderStatus.CANCELLED,
                order.getStatus()
        );

        verify(orderRepository, times(1))
                .save(order);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository, times(1))
                .save(captor.capture());

        OutboxEvent outboxEvent = captor.getValue();

        assertEquals("ORDER", outboxEvent.getAggregateType());
        assertEquals("100", outboxEvent.getAggregateId());
        assertEquals("ORDER_CANCELLED", outboxEvent.getEventType());
        assertEquals("order-cancelled", outboxEvent.getTopic());

        assertFalse(outboxEvent.isPublished());

        assertEquals(
                "{\"event\":\"order-cancelled\"}",
                outboxEvent.getPayload()
        );

        assertNotNull(outboxEvent.getCreatedAt());
    }

    @Test
    void markPaymentRefunded_shouldChangeCancelledOrderToRefunded() {

        // Arrange
        Long orderId = 100L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act
        orderService.markPaymentRefunded(orderId);

        // Assert
        assertEquals(
                OrderStatus.REFUNDED,
                order.getStatus()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, times(1))
                .save(order);
    }

    @Test
    void markPaymentRefunded_shouldDoNothing_whenOrderAlreadyRefunded() {

        // Arrange
        Long orderId = 100L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setStatus(OrderStatus.REFUNDED);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act
        orderService.markPaymentRefunded(orderId);

        // Assert
        assertEquals(
                OrderStatus.REFUNDED,
                order.getStatus()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void markPaymentRefunded_shouldThrowException_whenOrderIsNotCancelled() {

        // Arrange
        Long orderId = 100L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setStatus(OrderStatus.PAYMENT_SUCCESS);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act + Assert
        assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.markPaymentRefunded(orderId)
        );

        assertEquals(
                OrderStatus.PAYMENT_SUCCESS,
                order.getStatus()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void markPaymentRefunded_shouldThrowException_whenOrderNotFound() {

        // Arrange
        Long orderId = 999L;

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.markPaymentRefunded(orderId)
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
}
