package com.apiguard.backend.global.exception;

public class StatusPageNotFoundException extends RuntimeException {
    public StatusPageNotFoundException(String message) {
        super(message);
    }
}
