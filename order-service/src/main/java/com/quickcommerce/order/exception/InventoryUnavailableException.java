package com.quickcommerce.order.exception;

public class InventoryUnavailableException extends RuntimeException{
    public InventoryUnavailableException() {
        super("Inventory service is unavailable");
    }
}
