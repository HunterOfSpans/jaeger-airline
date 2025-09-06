package com.airline.flight.exception;

/**
 * 항공편을 찾을 수 없을 때 발생하는 예외
 * 
 * @author Claude Code
 * @since 1.0
 */
public class FlightNotFoundException extends RuntimeException {
    
    public FlightNotFoundException(String flightId) {
        super(String.format("Flight not found with ID: %s", flightId));
    }
    
    public FlightNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}