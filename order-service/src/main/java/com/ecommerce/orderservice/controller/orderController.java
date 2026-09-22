package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.orderRequest;
import com.ecommerce.orderservice.dto.orderResponse;
import com.ecommerce.orderservice.security.AuthenticatedUser;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class orderController {

    private final OrderService orderService;

    public orderController(OrderService orderService){
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<orderResponse> createOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody orderRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(user.userId(), user.email(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<orderResponse> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ADMIN")
                );

        return ResponseEntity.ok(
                orderService.getOrderById(
                        id,
                        user.userId(),
                        isAdmin
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<List<orderResponse>> getMyOrders(
            @AuthenticationPrincipal AuthenticatedUser user) {

        return ResponseEntity.ok(
                orderService.getOrderByUserId(user.userId())
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );

        orderService.cancelOrder(
                id,
                user.userId(),
                isAdmin
        );

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<orderResponse>> getAllOrders() {

        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
