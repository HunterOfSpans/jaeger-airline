package com.airline.flight.service;

import com.airline.flight.dto.AvailabilityRequest;
import com.airline.flight.dto.AvailabilityResponse;
import com.airline.flight.exception.InvalidRequestException;
import com.airline.flight.mapper.FlightMapper;
import com.airline.flight.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightServiceTest {

    private FlightService flightService;

    @BeforeEach
    void setUp() {
        flightService = new FlightService(new FlightRepository(), new FlightMapper());
    }

    @Test
    void checkAvailability_nullRequest_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> flightService.checkAvailability(null));
    }

    @Test
    void checkAvailability_nonPositiveSeats_throwsInvalidRequestException() {
        AvailabilityRequest request = new AvailabilityRequest("KE001", 0);
        assertThrows(InvalidRequestException.class, () -> flightService.checkAvailability(request));
    }

    @Test
    void checkAvailability_validRequest_returnsAvailableResponse() {
        AvailabilityRequest request = new AvailabilityRequest("KE001", 1);

        AvailabilityResponse response = flightService.checkAvailability(request);

        assertTrue(response.isAvailable());
        assertEquals("KE001", response.getFlightId());
        assertTrue(response.getAvailableSeats() >= 1);
    }
}
