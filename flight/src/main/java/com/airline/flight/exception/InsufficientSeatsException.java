package com.airline.flight.exception;

/**
 * 좌석이 부족할 때 발생하는 예외
 * 
 * @author Claude Code
 * @since 1.0
 */
public class InsufficientSeatsException extends RuntimeException {
    
    private final String flightId;
    private final int requestedSeats;
    private final int availableSeats;
    
    public InsufficientSeatsException(String flightId, int requestedSeats, int availableSeats) {
        super(String.format("Insufficient seats for flight %s. Requested: %d, Available: %d", 
                flightId, requestedSeats, availableSeats));
        this.flightId = flightId;
        this.requestedSeats = requestedSeats;
        this.availableSeats = availableSeats;
    }
    
    public String getFlightId() { return flightId; }
    public int getRequestedSeats() { return requestedSeats; }
    public int getAvailableSeats() { return availableSeats; }
}