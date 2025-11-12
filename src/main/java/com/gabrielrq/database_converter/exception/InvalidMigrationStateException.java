package com.gabrielrq.database_converter.exception;

public class InvalidMigrationStateException extends RuntimeException {
    public InvalidMigrationStateException(String message) {
        super(message);
    }
}
