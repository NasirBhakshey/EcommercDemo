package com.ecommerce.productservice.exception;

public class productNotFoundException extends RuntimeException{

    public productNotFoundException(String message){
        super(message);
    }
}
