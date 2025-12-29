package com.yowyob.rideandgo.domain.exception;

public class UserIsNotDriverException extends RuntimeException{
    public UserIsNotDriverException(String message) {
        super(message);
    }
}
