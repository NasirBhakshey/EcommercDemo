package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.InventoryReservation;
import com.ecommerce.productservice.entity.InventoryReservationStatus;
import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.entity.ProcessedInventoryEvent;
import com.ecommerce.productservice.event.InventoryReservedEvent;
import com.ecommerce.productservice.event.OrderCreatedEvent;
import com.ecommerce.productservice.event.OrderItemEvent;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.repository.InventoryReservationRepository;
import com.ecommerce.productservice.repository.OutboxEventRepository;
import com.ecommerce.productservice.repository.ProcessedInventoryEventRepository;
import com.ecommerce.productservice.repository.productRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InventoryService {

    private final productRepository productRepository;
    private final ProcessedInventoryEventRepository processedEventRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public InventoryService(productRepository productRepository,
                            ProcessedInventoryEventRepository processedEventRepository,
                            InventoryReservationRepository inventoryReservationRepository,
                            OutboxEventRepository outboxEventRepository,
                            ObjectMapper objectMapper
    ){
        this.productRepository = productRepository;
        this.processedEventRepository = processedEventRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean reduceStock(OrderCreatedEvent event){

        if(processedEventRepository.existsByOrderId(event.getOrderId())){
            System.out.println("Duplicate inventory event ignored. Order ID :" +event.getOrderId());
            return false;
        }

        if(event.getItems()==null || event.getItems().isEmpty()){
            throw new IllegalArgumentException("Order Contains No Items. Order ID: " +event.getOrderId());
        }

        for (OrderItemEvent item : event.getItems()){
            int update = productRepository.reduceStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            if(update == 0){
                throw new InsufficientStockException("Insufficient stock or product not found. Product ID: "
                        + item.getProductId());
            }

            InventoryReservation reservation = new InventoryReservation();
            reservation.setOrderId(event.getOrderId());
            reservation.setProductId(item.getProductId());
            reservation.setQuantity(item.getQuantity());
            reservation.setStatus(InventoryReservationStatus.RESERVED);

            inventoryReservationRepository.save(reservation);
        }

        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                UUID.randomUUID(),
                event.getOrderId(),
                event.getUserId(),
                event.getEmail(),
                event.getTotalAmount(),
                LocalDateTime.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId(
                String.valueOf(event.getOrderId())
        );
        outboxEvent.setEventType("INVENTORY_RESERVED");
        outboxEvent.setTopic("inventory-reserved");
        try{
            outboxEvent.setPayload(
                    objectMapper.writeValueAsString(reservedEvent)
            );
        }catch (JsonProcessingException e){
            throw new IllegalStateException(
                    "Failed to serialize InventoryReservedEvent. Order ID: "+event.getOrderId()
            );
        }
        outboxEvent.setPublished(false);
        outboxEvent.setCreatedAt(LocalDateTime.now());

        outboxEventRepository.save(outboxEvent);

        System.out.println("STOCK REDUCED SUCCESSFULLY. Order ID: " + event.getOrderId());

        return true;
    }

    @Transactional
    public boolean restoreStock(Long orderId) {

        var reservations =
                inventoryReservationRepository.findByOrderIdAndStatus(
                        orderId,
                        InventoryReservationStatus.RESERVED
                );

        if (reservations.isEmpty()) {
            System.out.println(
                    "No active inventory reservation found or stock already restored. Order ID: "
                            + orderId
            );

            return false;
        }

        for (InventoryReservation reservation : reservations) {

            int updated = productRepository.restoreStock(
                    reservation.getProductId(),
                    reservation.getQuantity()
            );

            if (updated == 0) {
                throw new IllegalStateException(
                        "Product not found while restoring stock. Product ID: "
                                + reservation.getProductId()
                );
            }

            reservation.setStatus(InventoryReservationStatus.RESTORED);
        }

        inventoryReservationRepository.saveAll(reservations);

        System.out.println("INVENTORY RESTORED AFTER PAYMENT FAILURE. Order ID: " + orderId);

        return true;
    }

    @Transactional
    public boolean commitStock(Long orderId) {

        var reservations =
                inventoryReservationRepository.findByOrderIdAndStatus(
                        orderId,
                        InventoryReservationStatus.RESERVED
                );

        if (reservations.isEmpty()) {
            System.out.println("No active reservation found or inventory already committed. Order ID: " + orderId);
            return false;
        }

        for (InventoryReservation reservation : reservations) {
            reservation.setStatus(InventoryReservationStatus.COMMITTED);
        }

        inventoryReservationRepository.saveAll(reservations);

        System.out.println("INVENTORY COMMITTED AFTER PAYMENT SUCCESS. Order ID: " + orderId);

        return true;
    }

    @Transactional
    public boolean restoreStockAfterOrderCancellation(Long orderId) {

        var reservations =
                inventoryReservationRepository.findByOrderIdAndStatus(
                        orderId,
                        InventoryReservationStatus.COMMITTED
                );

        if (reservations.isEmpty()) {

            System.out.println("No committed inventory found or cancellation already processed. Order ID: " + orderId);

            return false;
        }

        for (InventoryReservation reservation : reservations) {

            int updated = productRepository.restoreStock(
                    reservation.getProductId(),
                    reservation.getQuantity()
            );

            if (updated == 0) {
                throw new IllegalStateException(
                        "Product not found while restoring cancelled order stock. Product ID: "
                                + reservation.getProductId()
                );
            }

            reservation.setStatus(
                    InventoryReservationStatus.CANCELLED
            );
        }

        inventoryReservationRepository.saveAll(reservations);

        System.out.println(
                "INVENTORY RESTORED AFTER ORDER CANCELLATION. Order ID: "
                        + orderId
        );

        return true;
    }

}
