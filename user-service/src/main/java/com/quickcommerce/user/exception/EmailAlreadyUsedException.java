package com.quickcommerce.user.exception;

public class EmailAlreadyUsedException extends RuntimeException{
    public EmailAlreadyUsedException() { super("Email already registered"); }
}
