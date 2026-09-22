package com.ecommerce.orderservice.entity;

public enum OrderStatus {

    CREATED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    INVENTORY_FAILED,
    REFUNDED

}
