package com.airline.flight.domain.valueobject;

import lombok.Value;

/**
 * Airline Value Object
 */
@Value
public class Airline {
    String name;

    public static Airline of(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Airline name cannot be null or empty");
        }
        return new Airline(name.trim());
    }
}