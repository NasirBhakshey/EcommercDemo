package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class orderItemRequest {

    @NotNull(message = "Product Id is required...")
    private Long productId;
    @NotNull(message = "Quantity is Required...")
    @Min(value = 1, message = "Quantity Must be at-least 1...")
    private Integer quantity;
}
