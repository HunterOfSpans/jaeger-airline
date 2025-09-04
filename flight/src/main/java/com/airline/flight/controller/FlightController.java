package com.airline.flight.controller;

import com.airline.flight.dto.AvailabilityRequest;
import com.airline.flight.dto.AvailabilityResponse;
import com.airline.flight.dto.FlightDto;
import com.airline.flight.service.FlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/flights")
@RequiredArgsConstructor
public class FlightController {
    
    private final FlightService flightService;
    
    @GetMapping
    public ResponseEntity<List<FlightDto>> searchFlights(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String date) {
        
        if (from == null || to == null) {
            return ResponseEntity.ok(List.of());
        }
        
        List<FlightDto> flights = flightService.searchFlights(from, to, date);
        return ResponseEntity.ok(flights);
    }
    
    @GetMapping("/{flightId}")
    public ResponseEntity<FlightDto> getFlightById(@PathVariable String flightId) {
        log.info("Processing flight lookup for flightId: {}", flightId);
        FlightDto flight = flightService.getFlightById(flightId);
        
        if (flight == null) {
            log.warn("Flight not found: {}", flightId);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Flight lookup successful for flightId: {}, airline: {}, price: {}", 
                flightId, flight.getAirline(), flight.getPrice());
        return ResponseEntity.ok(flight);
    }
    
    @PostMapping("/{flightId}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable String flightId,
            @RequestBody AvailabilityRequest request) {
        
        request.setFlightId(flightId);
        AvailabilityResponse response = flightService.checkAvailability(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{flightId}/reserve")
    public ResponseEntity<Void> reserveSeats(
            @PathVariable String flightId,
            @RequestBody AvailabilityRequest request) {
        
        boolean success = flightService.reserveSeats(flightId, request.getRequestedSeats());
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
    
    @PostMapping("/{flightId}/release")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable String flightId,
            @RequestBody AvailabilityRequest request) {
        
        flightService.releaseSeats(flightId, request.getRequestedSeats());
        return ResponseEntity.ok().build();
    }
}