package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class productRequest {

    @NotBlank
    private String name;
    private String description;
    @NotNull
    @DecimalMin(value = "0.0",inclusive = true)
    private BigDecimal price;
    @NotNull
    @Min(0)
    private Integer stock;

}
