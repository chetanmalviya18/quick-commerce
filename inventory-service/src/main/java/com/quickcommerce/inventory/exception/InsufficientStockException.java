package com.quickcommerce.inventory.exception;

public class InsufficientStockException extends RuntimeException{
    public  InsufficientStockException() {
        super("Not enough stock");
    }
}
