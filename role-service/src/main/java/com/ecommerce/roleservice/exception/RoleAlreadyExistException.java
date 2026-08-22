package com.ecommerce.roleservice.exception;

public class RoleAlreadyExistException extends RuntimeException{

    public RoleAlreadyExistException(String message){
        super(message);
    }
}
