package com.airline.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private boolean available;
    private String flightId;
    private Integer availableSeats;
    private String message;
}