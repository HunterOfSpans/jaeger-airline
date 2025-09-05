package com.airline.flight.service;

import com.airline.flight.dto.AvailabilityRequest;
import com.airline.flight.dto.AvailabilityResponse;
import com.airline.flight.dto.FlightDto;
import com.airline.flight.entity.Flight;
import com.airline.flight.mapper.FlightMapper;
import com.airline.flight.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {
    
    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;
    
    public List<FlightDto> searchFlights(String from, String to, String date) {
        log.info("Searching flights from {} to {} on {}", from, to, date);
        List<Flight> flights = flightRepository.findByDepartureAndArrival(from, to);
        return flightMapper.toDtoList(flights);
    }
    
    public FlightDto getFlightById(String flightId) {
        log.info("Getting flight by ID: {}", flightId);
        Optional<Flight> flight = flightRepository.findById(flightId);
        return flight.map(flightMapper::toDto).orElse(null);
    }
    
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        log.info("Checking availability for flight {} with {} seats", 
                request.getFlightId(), request.getRequestedSeats());
        
        Optional<Flight> flightOpt = flightRepository.findById(request.getFlightId());
        if (flightOpt.isEmpty()) {
            return new AvailabilityResponse(false, request.getFlightId(), 0, "Flight not found");
        }
        
        Flight flight = flightOpt.get();
        boolean available = flight.getAvailableSeats() >= request.getRequestedSeats();
        String message = available ? "Seats available" : "Not enough seats available";
        
        return new AvailabilityResponse(available, request.getFlightId(), 
                                      flight.getAvailableSeats(), message);
    }
    
    public boolean reserveSeats(String flightId, Integer seats) {
        log.info("Reserving {} seats for flight {}", seats, flightId);
        Optional<Flight> flightOpt = flightRepository.findById(flightId);
        
        if (flightOpt.isEmpty()) {
            log.warn("Flight not found: {}", flightId);
            return false;
        }
        
        Flight flight = flightOpt.get();
        if (flight.getAvailableSeats() >= seats) {
            flight.setAvailableSeats(flight.getAvailableSeats() - seats);
            flightRepository.save(flight);
            log.info("Successfully reserved {} seats. Available seats now: {}", 
                    seats, flight.getAvailableSeats());
            return true;
        }
        
        log.warn("Failed to reserve {} seats for flight {}", seats, flightId);
        return false;
    }
    
    public void releaseSeats(String flightId, Integer seats) {
        log.info("Releasing {} seats for flight {}", seats, flightId);
        Optional<Flight> flightOpt = flightRepository.findById(flightId);
        
        if (flightOpt.isPresent()) {
            Flight flight = flightOpt.get();
            flight.setAvailableSeats(flight.getAvailableSeats() + seats);
            flightRepository.save(flight);
            log.info("Successfully released {} seats. Available seats now: {}", 
                    seats, flight.getAvailableSeats());
        } else {
            log.warn("Flight not found for releasing seats: {}", flightId);
        }
    }
}