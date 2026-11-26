package com.quickcommerce.inventory.exception;

public class ReservationNotFoundException extends RuntimeException{
    public ReservationNotFoundException() {
        super("Reservation not found");
    }
}
