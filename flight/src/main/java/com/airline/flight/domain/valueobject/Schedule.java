package com.airline.flight.domain.valueobject;

import lombok.Value;

import java.time.LocalDateTime;

/**
 * Flight Schedule Value Object
 */
@Value
public class Schedule {
    LocalDateTime departureTime;
    LocalDateTime arrivalTime;

    public static Schedule of(LocalDateTime departureTime, LocalDateTime arrivalTime) {
        if (departureTime == null) {
            throw new IllegalArgumentException("Departure time cannot be null");
        }
        if (arrivalTime == null) {
            throw new IllegalArgumentException("Arrival time cannot be null");
        }
        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }

        return new Schedule(departureTime, arrivalTime);
    }

    public boolean isBeforeDeparture() {
        return LocalDateTime.now().isBefore(departureTime);
    }

    public long getFlightDurationMinutes() {
        return java.time.Duration.between(departureTime, arrivalTime).toMinutes();
    }
}