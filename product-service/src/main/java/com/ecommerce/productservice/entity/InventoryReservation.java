package com.ecommerce.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "inventory_reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_reservation_order_product",
                        columnNames = {"order_id", "product_id"}
                )
        }
)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(nullable = false)
    private Integer quantity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryReservationStatus status;
}
