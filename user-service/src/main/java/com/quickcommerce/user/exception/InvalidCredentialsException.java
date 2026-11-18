package com.quickcommerce.user.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Wrong email or password");
    }
}
