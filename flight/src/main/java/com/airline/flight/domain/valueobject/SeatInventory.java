package com.airline.flight.domain.valueobject;

import lombok.Value;

/**
 * Seat Inventory Value Object
 */
@Value(staticConstructor = "of")
public class SeatInventory {
    int totalSeats;
    int availableSeats;

    public static SeatInventory create(int totalSeats) {
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("Total seats must be greater than zero");
        }
        return new SeatInventory(totalSeats, totalSeats);
    }

    public static SeatInventory reconstruct(int totalSeats, int availableSeats) {
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("Total seats must be greater than zero");
        }
        if (availableSeats < 0) {
            throw new IllegalArgumentException("Available seats cannot be negative");
        }
        if (availableSeats > totalSeats) {
            throw new IllegalArgumentException("Available seats cannot exceed total seats");
        }
        return new SeatInventory(totalSeats, availableSeats);
    }

    public boolean canReserve(int requestedSeats) {
        return availableSeats >= requestedSeats && requestedSeats > 0;
    }

    public SeatInventory reserve(int seatCount) {
        if (!canReserve(seatCount)) {
            throw new IllegalStateException(
                String.format("Cannot reserve %d seats. Available: %d", seatCount, availableSeats)
            );
        }
        return new SeatInventory(totalSeats, availableSeats - seatCount);
    }

    public SeatInventory release(int seatCount) {
        if (seatCount <= 0) {
            throw new IllegalArgumentException("Seat count to release must be greater than zero");
        }

        int newAvailableSeats = availableSeats + seatCount;
        if (newAvailableSeats > totalSeats) {
            throw new IllegalStateException(
                String.format("Cannot release %d seats. Would exceed total seats: %d",
                    seatCount, totalSeats)
            );
        }

        return new SeatInventory(totalSeats, newAvailableSeats);
    }
}