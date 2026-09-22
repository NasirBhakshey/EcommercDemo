package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class orderItemResponse {

    private Long id;
    private Long productId;
    private Integer quantity;
    private BigDecimal price;


}
