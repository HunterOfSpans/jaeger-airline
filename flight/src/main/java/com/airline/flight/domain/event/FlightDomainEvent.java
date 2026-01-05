package com.airline.flight.domain.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Flight Domain Events
 */
public abstract class FlightDomainEvent {
    public abstract String getEventId();
    public abstract String getAggregateId();
    public abstract Instant getTimestamp();
    public abstract String getEventType();

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class FlightCreated extends FlightDomainEvent {
        String eventId;
        String aggregateId;
        Instant timestamp;
        String airlineName;
        String departureCode;
        String arrivalCode;

        public static FlightCreated of(String flightId, String airlineName, String departureCode, String arrivalCode) {
            return new FlightCreated(
                UUID.randomUUID().toString(),
                flightId,
                Instant.now(),
                airlineName,
                departureCode,
                arrivalCode
            );
        }

        @Override
        public String getEventType() {
            return "FlightCreated";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SeatsReserved extends FlightDomainEvent {
        String eventId;
        String aggregateId;
        Instant timestamp;
        int reservedSeats;
        int remainingSeats;

        public static SeatsReserved of(String flightId, int reservedSeats, int remainingSeats) {
            return new SeatsReserved(
                UUID.randomUUID().toString(),
                flightId,
                Instant.now(),
                reservedSeats,
                remainingSeats
            );
        }

        @Override
        public String getEventType() {
            return "SeatsReserved";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SeatsReleased extends FlightDomainEvent {
        String eventId;
        String aggregateId;
        Instant timestamp;
        int releasedSeats;
        int availableSeats;

        public static SeatsReleased of(String flightId, int releasedSeats, int availableSeats) {
            return new SeatsReleased(
                UUID.randomUUID().toString(),
                flightId,
                Instant.now(),
                releasedSeats,
                availableSeats
            );
        }

        @Override
        public String getEventType() {
            return "SeatsReleased";
        }
    }
}