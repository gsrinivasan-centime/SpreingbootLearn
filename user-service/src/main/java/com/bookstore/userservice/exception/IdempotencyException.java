package com.bookstore.userservice.exception;

public class IdempotencyException extends RuntimeException {
    
    public IdempotencyException(String message) {
        super(message);
    }
    
    public IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
