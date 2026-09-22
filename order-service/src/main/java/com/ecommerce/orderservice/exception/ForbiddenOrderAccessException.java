package com.ecommerce.orderservice.exception;

public class ForbiddenOrderAccessException extends RuntimeException{

    public ForbiddenOrderAccessException(String message) {
        super(message);
    }
}
