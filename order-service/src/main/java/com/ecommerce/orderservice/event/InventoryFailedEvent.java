package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryFailedEvent {

    private UUID eventId;

    private Long orderId;
    private Long userId;
    private String email;
    private String reason;
    private LocalDateTime failedAt;
}
