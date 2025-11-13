package com.gabrielrq.database_converter.exception;

public class ConsistencyValidationException extends RuntimeException {
    public ConsistencyValidationException(String message) {
        super(message);
    }
}
