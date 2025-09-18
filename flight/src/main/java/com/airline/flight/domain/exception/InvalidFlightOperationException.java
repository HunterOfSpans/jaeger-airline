package com.airline.flight.domain.exception;

/**
 * Domain Exception for invalid flight operations
 */
public class InvalidFlightOperationException extends RuntimeException {
    public InvalidFlightOperationException(String message) {
        super(message);
    }

    public InvalidFlightOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}