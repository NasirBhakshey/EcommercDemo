package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.event.InventoryFailedEvent;
import com.ecommerce.productservice.event.OrderCreatedEvent;
import com.ecommerce.productservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InventoryFailureService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public InventoryFailureService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFailureEvent(OrderCreatedEvent orderEvent, String reason) {

        InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                        UUID.randomUUID(),
                        orderEvent.getOrderId(),
                        orderEvent.getUserId(),
                        orderEvent.getEmail(),
                        reason,
                        LocalDateTime.now()
                );

        try {

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setAggregateType("INVENTORY");

            outboxEvent.setAggregateId(String.valueOf(orderEvent.getOrderId()));

            outboxEvent.setEventType("INVENTORY_FAILED");

            outboxEvent.setTopic("inventory-failed");

            outboxEvent.setPayload(objectMapper.writeValueAsString(failedEvent));

            outboxEvent.setPublished(false);
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new IllegalStateException("Failed to create inventory failure outbox event", e);
        }
    }
}
