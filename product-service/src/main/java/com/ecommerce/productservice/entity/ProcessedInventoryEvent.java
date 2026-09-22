package com.ecommerce.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_inventory_events",
       uniqueConstraints = {
             @UniqueConstraint(name = "uk_processed_inventory_order",
                               columnNames = "order_id"
             )
}
)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedInventoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
