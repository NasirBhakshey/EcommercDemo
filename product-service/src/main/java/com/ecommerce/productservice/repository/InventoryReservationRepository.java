package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.InventoryReservation;
import com.ecommerce.productservice.entity.InventoryReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByOrderIdAndStatus(Long Id, InventoryReservationStatus status);
}
