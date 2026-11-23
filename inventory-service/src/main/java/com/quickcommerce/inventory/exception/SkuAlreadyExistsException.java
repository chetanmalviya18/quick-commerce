package com.quickcommerce.inventory.exception;

public class SkuAlreadyExistsException extends RuntimeException {
    public SkuAlreadyExistsException() { super("SKU already exists"); }
}
