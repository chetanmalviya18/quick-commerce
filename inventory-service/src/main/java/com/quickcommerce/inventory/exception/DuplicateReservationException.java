package com.quickcommerce.inventory.exception;

public class DuplicateReservationException extends RuntimeException{
    public  DuplicateReservationException() {
        super("A reservation for this order is already being created");
    }
}
