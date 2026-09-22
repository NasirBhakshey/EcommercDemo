package com.ecommerce.productservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservedEvent {

    private UUID eventId;

    private Long orderId;
    private Long userId;
    private String email;
    private BigDecimal totalAmount;
    private LocalDateTime reservedAt;
}
