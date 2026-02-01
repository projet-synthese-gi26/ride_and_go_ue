package com.yowyob.rideandgo.domain.exception;

public class DriverProfileNotValidatedException extends RuntimeException {
    public DriverProfileNotValidatedException(String message) {
        super(message);
    }
}