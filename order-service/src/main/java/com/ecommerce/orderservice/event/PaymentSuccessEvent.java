package com.ecommerce.orderservice.event;

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
public class PaymentSuccessEvent {

    private UUID eventId;

    private Long paymentId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;
    private LocalDateTime paidAt;
}
