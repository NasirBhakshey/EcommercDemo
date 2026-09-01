package com.ecommerce.productservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent {

    private Long paymentId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime failedAt;
}
