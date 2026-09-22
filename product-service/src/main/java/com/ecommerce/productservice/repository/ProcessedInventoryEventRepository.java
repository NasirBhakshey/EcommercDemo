package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.ProcessedInventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedInventoryEventRepository extends JpaRepository<ProcessedInventoryEvent, Long> {

    boolean existsByOrderId(Long orderId);
}
