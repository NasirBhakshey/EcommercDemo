package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface productRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock - :quantity
            WHERE p.id = :productId
            AND p.stock >= :quantity
            """)
    int reduceStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    @Modifying
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock + :quantity
            WHERE p.id = :productId
            """)
    int restoreStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );
}
