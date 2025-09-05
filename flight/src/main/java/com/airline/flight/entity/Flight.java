package com.airline.flight.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flight {
    private String flightId;
    private String airline;
    private String departure;
    private String arrival;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal price;
    private Integer totalSeats;
    private Integer availableSeats;
    private String aircraft;
    
    public Flight(String flightId, String airline, String departure, String arrival,
                 LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal price,
                 Integer availableSeats, String aircraft) {
        this.flightId = flightId;
        this.airline = airline;
        this.departure = departure;
        this.arrival = arrival;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.totalSeats = availableSeats;
        this.availableSeats = availableSeats;
        this.aircraft = aircraft;
    }
}