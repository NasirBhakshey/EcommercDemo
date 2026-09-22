package com.ecommerce.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class orderRequest {

    @Valid
    @NotEmpty(message = "Order Must Contain at-least 1 Item...")
    private List<orderItemRequest> items;
}
