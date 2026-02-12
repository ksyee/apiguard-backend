package com.apiguard.backend.global.exception;

public class EndpointNotFoundException extends RuntimeException {
    public EndpointNotFoundException(String message) {
        super(message);
    }
}
