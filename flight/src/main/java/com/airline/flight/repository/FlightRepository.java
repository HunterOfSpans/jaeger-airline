package com.airline.flight.repository;

import com.airline.flight.entity.Flight;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class FlightRepository {
    
    private final Map<String, Flight> flights = new ConcurrentHashMap<>();
    
    public FlightRepository() {
        initializeFlights();
    }
    
    private void initializeFlights() {
        save(new Flight("KE001", "Korean Air", "ICN", "PUS",
            LocalDateTime.now().plusHours(3),
            LocalDateTime.now().plusHours(4).plusMinutes(30),
            new BigDecimal("120000"), 180, "Boeing 737"));
        
        save(new Flight("KE123", "Korean Air", "ICN", "NRT",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2),
            new BigDecimal("350000"), 150, "Boeing 787"));
        
        save(new Flight("OZ456", "Asiana Airlines", "ICN", "LAX", 
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(12),
            new BigDecimal("850000"), 200, "Airbus A350"));
        
        save(new Flight("LH789", "Lufthansa", "ICN", "FRA",
            LocalDateTime.now().plusDays(3),
            LocalDateTime.now().plusDays(3).plusHours(11),
            new BigDecimal("950000"), 180, "Boeing 747"));
        
        save(new Flight("NON_EXISTENT_FLIGHT", "Test Airline", "ICN", "TEST",
            LocalDateTime.now().plusDays(10),
            LocalDateTime.now().plusDays(10).plusHours(1),
            new BigDecimal("1"), 1, "Test Aircraft"));
    }
    
    public Flight save(Flight flight) {
        flights.put(flight.getFlightId(), flight);
        return flight;
    }
    
    public Optional<Flight> findById(String flightId) {
        return Optional.ofNullable(flights.get(flightId));
    }
    
    public List<Flight> findAll() {
        return flights.values().stream().collect(Collectors.toList());
    }
    
    public List<Flight> findByDepartureAndArrival(String departure, String arrival) {
        return flights.values().stream()
            .filter(flight -> flight.getDeparture().equalsIgnoreCase(departure) && 
                             flight.getArrival().equalsIgnoreCase(arrival))
            .collect(Collectors.toList());
    }
    
    public boolean existsById(String flightId) {
        return flights.containsKey(flightId);
    }
    
    public void deleteById(String flightId) {
        flights.remove(flightId);
    }
    
    public long count() {
        return flights.size();
    }
}