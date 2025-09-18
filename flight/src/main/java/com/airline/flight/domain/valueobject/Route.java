package com.airline.flight.domain.valueobject;

import lombok.Value;

/**
 * Flight Route Value Object
 */
@Value
public class Route {
    Airport departure;
    Airport arrival;

    public static Route of(Airport departure, Airport arrival) {
        if (departure == null) {
            throw new IllegalArgumentException("Departure airport cannot be null");
        }
        if (arrival == null) {
            throw new IllegalArgumentException("Arrival airport cannot be null");
        }
        if (departure.equals(arrival)) {
            throw new IllegalArgumentException("Departure and arrival airports cannot be the same");
        }

        return new Route(departure, arrival);
    }
}