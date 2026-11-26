package com.quickcommerce.inventory.exception;

public class ReservationNotActiveException extends RuntimeException{
    public  ReservationNotActiveException() {
        super("Reservation is no longer active");
    }
}
