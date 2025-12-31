package com.yowyob.rideandgo.domain.exception;

public class OfferNotFoundException extends RuntimeException {
    public OfferNotFoundException(String message) {
        super(message);
    }
}