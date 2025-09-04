package com.airline.flight.service;

import com.airline.flight.dto.AvailabilityRequest;
import com.airline.flight.dto.AvailabilityResponse;
import com.airline.flight.dto.FlightDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FlightService {
    
    private final Map<String, FlightDto> flights = new ConcurrentHashMap<>();
    
    public FlightService() {
        initializeFlights();
    }
    
    private void initializeFlights() {
        // 분산 추적 테스트용 항공편
        flights.put("KE001", new FlightDto(
            "KE001", "Korean Air", "ICN", "PUS",
            LocalDateTime.now().plusHours(3),
            LocalDateTime.now().plusHours(4).plusMinutes(30),
            new BigDecimal("120000"), 180, "Boeing 737"
        ));
        
        flights.put("KE123", new FlightDto(
            "KE123", "Korean Air", "ICN", "NRT",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2),
            new BigDecimal("350000"), 150, "Boeing 787"
        ));
        
        flights.put("OZ456", new FlightDto(
            "OZ456", "Asiana Airlines", "ICN", "LAX", 
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(12),
            new BigDecimal("850000"), 200, "Airbus A350"
        ));
        
        flights.put("LH789", new FlightDto(
            "LH789", "Lufthansa", "ICN", "FRA",
            LocalDateTime.now().plusDays(3),
            LocalDateTime.now().plusDays(3).plusHours(11),
            new BigDecimal("950000"), 180, "Boeing 747"
        ));
        
        // 추가 분산 추적 테스트용 항공편들
        flights.put("NON_EXISTENT_FLIGHT", new FlightDto(
            "NON_EXISTENT_FLIGHT", "Test Airline", "ICN", "TEST",
            LocalDateTime.now().plusDays(10),
            LocalDateTime.now().plusDays(10).plusHours(1),
            new BigDecimal("1"), 1, "Test Aircraft"
        ));
    }
    
    public List<FlightDto> searchFlights(String from, String to, String date) {
        log.info("Searching flights from {} to {} on {}", from, to, date);
        return flights.values().stream()
            .filter(flight -> flight.getDeparture().equalsIgnoreCase(from) && 
                             flight.getArrival().equalsIgnoreCase(to))
            .toList();
    }
    
    public FlightDto getFlightById(String flightId) {
        log.info("Getting flight by ID: {}", flightId);
        return flights.get(flightId);
    }
    
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        log.info("Checking availability for flight {} with {} seats", 
                request.getFlightId(), request.getRequestedSeats());
        
        FlightDto flight = flights.get(request.getFlightId());
        if (flight == null) {
            return new AvailabilityResponse(false, request.getFlightId(), 0, "Flight not found");
        }
        
        boolean available = flight.getAvailableSeats() >= request.getRequestedSeats();
        String message = available ? "Seats available" : "Not enough seats available";
        
        return new AvailabilityResponse(available, request.getFlightId(), 
                                      flight.getAvailableSeats(), message);
    }
    
    public boolean reserveSeats(String flightId, Integer seats) {
        log.info("Reserving {} seats for flight {}", seats, flightId);
        FlightDto flight = flights.get(flightId);
        if (flight != null && flight.getAvailableSeats() >= seats) {
            flight.setAvailableSeats(flight.getAvailableSeats() - seats);
            log.info("Successfully reserved {} seats. Available seats now: {}", 
                    seats, flight.getAvailableSeats());
            return true;
        }
        log.warn("Failed to reserve {} seats for flight {}", seats, flightId);
        return false;
    }
    
    public void releaseSeats(String flightId, Integer seats) {
        log.info("Releasing {} seats for flight {}", seats, flightId);
        FlightDto flight = flights.get(flightId);
        if (flight != null) {
            flight.setAvailableSeats(flight.getAvailableSeats() + seats);
            log.info("Successfully released {} seats. Available seats now: {}", 
                    seats, flight.getAvailableSeats());
        }
    }
}