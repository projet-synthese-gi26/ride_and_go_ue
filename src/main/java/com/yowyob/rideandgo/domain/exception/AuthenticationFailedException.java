package com.yowyob.rideandgo.domain.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}