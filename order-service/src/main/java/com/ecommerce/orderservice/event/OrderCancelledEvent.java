package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancelledEvent {

    private UUID eventId;

    private Long orderId;
    private Long userId;
    private List<OrderItemEvent> items;
    private LocalDateTime cancelledAt;
}
