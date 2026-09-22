package com.ecommerce.orderservice.security;

public record AuthenticatedUser(
        Long userId,
        String email
) {
}
