package com.airline.flight.domain.valueobject;

import lombok.Value;

/**
 * Aircraft Value Object
 */
@Value
public class Aircraft {
    String type;

    public static Aircraft of(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Aircraft type cannot be null or empty");
        }
        return new Aircraft(type.trim());
    }
}