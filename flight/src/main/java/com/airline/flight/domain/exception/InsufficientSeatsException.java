package com.airline.flight.domain.exception;

/**
 * Domain Exception for insufficient seats
 */
public class InsufficientSeatsException extends RuntimeException {
    public InsufficientSeatsException(String message) {
        super(message);
    }

    public InsufficientSeatsException(String message, Throwable cause) {
        super(message, cause);
    }
}