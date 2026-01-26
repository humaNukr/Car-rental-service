package com.example.carrental.exception.rental;

public class RentalAlreadyFinishedException extends RuntimeException {
    public RentalAlreadyFinishedException(String message) {
        super(message);
    }
}
