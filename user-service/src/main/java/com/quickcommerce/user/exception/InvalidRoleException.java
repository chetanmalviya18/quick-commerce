package com.quickcommerce.user.exception;

public class InvalidRoleException extends RuntimeException{
    public InvalidRoleException(){
        super("Role not allowed");
    }
}
