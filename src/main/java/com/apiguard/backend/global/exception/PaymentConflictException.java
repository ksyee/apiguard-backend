package com.apiguard.backend.global.exception;

public class PaymentConflictException extends PaymentException {

    public PaymentConflictException(String message) {
        super(message);
    }
}
