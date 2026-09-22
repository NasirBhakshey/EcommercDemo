package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.orderRequest;
import com.ecommerce.orderservice.dto.orderResponse;

import java.util.List;

public interface OrderService {

    orderResponse createOrder(Long userId, String email, orderRequest request);

    orderResponse getOrderById(Long id, Long userId, boolean isAdmin);

    List<orderResponse> getAllOrders();

    List<orderResponse> getOrderByUserId(Long userId);

    void cancelOrder(Long id, Long userId, boolean isAdmin);

    void markPaymentSuccess(Long orderId);

    void markPaymentFailed(Long orderId);

    void markInventoryFailed(Long orderId);

    void markPaymentRefunded(Long orderId);
}
