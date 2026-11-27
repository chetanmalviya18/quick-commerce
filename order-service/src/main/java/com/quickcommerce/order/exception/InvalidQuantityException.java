package com.quickcommerce.order.exception;

public class InvalidQuantityException extends RuntimeException{
    public InvalidQuantityException() {
        super("Invalid quantity");
    }
}
