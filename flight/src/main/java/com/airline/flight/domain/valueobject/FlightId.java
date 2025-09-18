package com.airline.flight.domain.valueobject;

import lombok.Value;

import java.util.Objects;

/**
 * Flight ID Value Object
 */
@Value
public class FlightId {
    String value;

    public static FlightId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Flight ID cannot be null or empty");
        }
        return new FlightId(value.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightId flightId = (FlightId) o;
        return Objects.equals(value, flightId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}