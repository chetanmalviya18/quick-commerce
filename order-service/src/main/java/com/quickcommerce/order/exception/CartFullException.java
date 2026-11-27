package com.quickcommerce.order.exception;

public class CartFullException extends RuntimeException {
    public CartFullException() {
        super("Cart has too many different products");
    }
}
