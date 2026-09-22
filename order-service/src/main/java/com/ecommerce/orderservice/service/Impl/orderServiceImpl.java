package com.ecommerce.orderservice.service.Impl;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.event.OrderItemEvent;
import com.ecommerce.orderservice.exception.ForbiddenOrderAccessException;
import com.ecommerce.orderservice.exception.InvalidOrderStateException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OutboxEventRepository;
import com.ecommerce.orderservice.repository.orderRepository;
import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class orderServiceImpl implements OrderService {

    private final orderRepository orderRepository;
    private final ProductClient productClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public orderServiceImpl(orderRepository orderRepository, ProductClient productClient,
                            OutboxEventRepository outboxEventRepository,
                            ObjectMapper objectMapper){
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public orderResponse createOrder(Long userId, String email, orderRequest request) {
        Order order=new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for(orderItemRequest itemRequest : request.getItems()){

            ProductResponse product = productClient.getProductById(
                    itemRequest.getProductId()
            );

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());

            order.addItem(orderItem);

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        Order saveOrder = orderRepository.save(order);

        List<OrderItemEvent> eventItems = saveOrder.getItems()
                .stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();
        OrderCreatedEvent event=new OrderCreatedEvent(
                UUID.randomUUID(),
                saveOrder.getId(),
                saveOrder.getUserId(),
                email,
                saveOrder.getTotalAmount(),
                saveOrder.getStatus().name(),
                saveOrder.getCreatedAt(),
                eventItems
        );

        try {

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setAggregateType("ORDER");
            outboxEvent.setAggregateId(
                    String.valueOf(saveOrder.getId())
            );

            outboxEvent.setEventType("ORDER_CREATED");
            outboxEvent.setTopic("order-created");

            outboxEvent.setPayload(
                    objectMapper.writeValueAsString(event)
            );

            outboxEvent.setPublished(false);
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to create order-created outbox event",
                    e
            );
        }
        return mapToResponse(saveOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public orderResponse getOrderById(Long id, Long userId, boolean isAdmin) {

        Order order = orderRepository.findById(id).orElseThrow(() ->
                        new OrderNotFoundException("Order Not Found With Id: " + id));

        boolean isOwner = Objects.equals(order.getUserId(), userId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenOrderAccessException("You are not allowed to access this order");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<orderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<orderResponse> getOrderByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelOrder(Long id, Long userId, boolean isAdmin) {

        Order order = orderRepository.findById(id).orElseThrow(() ->
                        new OrderNotFoundException("Order Not Found With Id: " + id));

        boolean isOwner = Objects.equals(order.getUserId(), userId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenOrderAccessException("You are not allowed to cancel this order");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ForbiddenOrderAccessException("Delivered Order Cannot be Cancelled...");
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        List<OrderItemEvent> items = order.getItems()
                .stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        OrderCancelledEvent cancelledEvent =
                new OrderCancelledEvent(
                        UUID.randomUUID(),
                        order.getId(),
                        order.getUserId(),
                        items,
                        LocalDateTime.now()
                );

        try {

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setAggregateType("ORDER");
            outboxEvent.setAggregateId(
                    String.valueOf(order.getId())
            );

            outboxEvent.setEventType("ORDER_CANCELLED");
            outboxEvent.setTopic("order-cancelled");

            outboxEvent.setPayload(
                    objectMapper.writeValueAsString(cancelledEvent)
            );

            outboxEvent.setPublished(false);
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to create order cancellation outbox event",
                    e
            );
        }
    }

    @Override
    @Transactional
    public void markPaymentSuccess(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId
                        )
                );

        order.setStatus(OrderStatus.PAYMENT_SUCCESS);

        orderRepository.save(order);

        System.out.println(
                "ORDER STATUS UPDATED TO PAYMENT_SUCCESS. Order ID: "
                        + orderId
        );
    }

    @Override
    @Transactional
    public void markPaymentFailed(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId
                        )
                );

        order.setStatus(OrderStatus.PAYMENT_FAILED);

        orderRepository.save(order);

        System.out.println(
                "ORDER STATUS UPDATED TO PAYMENT_FAILED. Order ID: "
                        + orderId
        );
    }

    @Override
    @Transactional
    public void markInventoryFailed(Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalStateException(
                                "Order not found. Order ID: " + orderId));
        order.setStatus(OrderStatus.INVENTORY_FAILED);

        orderRepository.save(order);

        System.out.println("ORDER STATUS UPDATED TO INVENTORY_FAILED. Order ID: " + orderId);

    }

    @Override
    @Transactional
    public void markPaymentRefunded(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found with id: " + orderId
                        )
                );

        // Idempotency
        if (order.getStatus() == OrderStatus.REFUNDED) {

            System.out.println(
                    "Duplicate payment-refunded event ignored. Order ID: "
                            + orderId
            );

            return;
        }

        if (order.getStatus() != OrderStatus.CANCELLED) {

            throw new InvalidOrderStateException(
                    "Order must be CANCELLED before refund can be completed. "
                            + "Order ID: " + orderId
                            + ", Current status: " + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.REFUNDED);

        orderRepository.save(order);

        System.out.println(
                "ORDER STATUS UPDATED TO REFUNDED. Order ID: "
                        + orderId
        );
    }

    private orderResponse mapToResponse(Order order){
        List<orderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> new orderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()
                )).toList();

        return new orderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}
