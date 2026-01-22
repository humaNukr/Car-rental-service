package com.example.carrental.exception.car;

public class LicensePlateAlreadyExistsException extends RuntimeException {

    public LicensePlateAlreadyExistsException(String message) {
        super(message);
    }
}
