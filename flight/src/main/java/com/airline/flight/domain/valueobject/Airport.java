package com.airline.flight.domain.valueobject;

import lombok.Value;

/**
 * Airport Value Object
 */
@Value
public class Airport {
    String code;

    public static Airport of(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Airport code cannot be null or empty");
        }

        String trimmedCode = code.trim().toUpperCase();
        if (trimmedCode.length() != 3) {
            throw new IllegalArgumentException("Airport code must be exactly 3 characters");
        }

        return new Airport(trimmedCode);
    }
}