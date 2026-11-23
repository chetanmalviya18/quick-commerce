package com.quickcommerce.inventory.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException() { super("Product not found"); }
}
