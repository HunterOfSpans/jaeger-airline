package com.airline.flight.service;

import com.airline.flight.domain.model.FlightAggregate;
import com.airline.flight.domain.repository.FlightDomainRepository;
import com.airline.flight.domain.service.FlightDomainService;
import com.airline.flight.domain.valueobject.FlightId;
import com.airline.flight.domain.valueobject.Route;
import com.airline.flight.domain.valueobject.Airport;
import com.airline.flight.domain.exception.InsufficientSeatsException;
import com.airline.flight.domain.exception.InvalidFlightOperationException;
import com.airline.flight.dto.AvailabilityRequest;
import com.airline.flight.dto.AvailabilityResponse;
import com.airline.flight.dto.FlightDto;
import com.airline.flight.exception.FlightNotFoundException;
import com.airline.flight.exception.InvalidRequestException;
import com.airline.flight.mapper.FlightMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Flight Application Service
 * 
 * DDD 패턴에 따른 애플리케이션 서비스
 * 도메인 로직은 도메인 서비스와 애그리게이트에 위임
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {
    
    private final FlightDomainRepository flightDomainRepository;
    private final FlightDomainService flightDomainService;
    private final FlightMapper flightMapper;
    
    /**
     * 항공편 검색
     */
    public List<FlightDto> searchFlights(String from, String to, String date) {
        log.info("Searching flights from {} to {} on {}", from, to, date);
        
        validateSearchParameters(from, to);
        
        List<FlightAggregate> flights = flightDomainRepository.findByDepartureAndArrival(from, to);
        
        return flights.stream()
                .map(flightMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 항공편 ID로 조회
     */
    public FlightDto getFlightById(String flightId) {
        log.info("Getting flight by ID: {}", flightId);
        
        validateFlightId(flightId);
        
        FlightId domainFlightId = FlightId.of(flightId);
        Optional<FlightAggregate> flight = flightDomainRepository.findById(domainFlightId);
        
        return flight.map(flightMapper::toDto)
                .orElseThrow(() -> new FlightNotFoundException(flightId));
    }
    
    /**
     * 좌석 가용성 확인
     */
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        validateAvailabilityRequest(request);
        
        log.info("Checking availability for flight {} with {} seats",
                request.getFlightId(), request.getRequestedSeats());

        FlightId flightId = FlightId.of(request.getFlightId());
        Optional<FlightAggregate> flightOpt = flightDomainRepository.findById(flightId);
        
        if (flightOpt.isEmpty()) {
            throw new FlightNotFoundException(request.getFlightId());
        }
        
        FlightAggregate flight = flightOpt.get();
        boolean available = flight.isAvailable(request.getRequestedSeats());
        String message = available ? "Seats available" : "Not enough seats available";
        
        return new AvailabilityResponse(available, request.getFlightId(), 
                                      flight.getAvailableSeats(), message);
    }
    
    /**
     * 좌석 예약
     */
    @Transactional
    public void reserveSeats(String flightId, Integer seats) {
        log.info("Reserving {} seats for flight {}", seats, flightId);
        
        validateReservationParameters(flightId, seats);
        
        FlightId domainFlightId = FlightId.of(flightId);
        Optional<FlightAggregate> flightOpt = flightDomainRepository.findById(domainFlightId);
        
        if (flightOpt.isEmpty()) {
            throw new FlightNotFoundException(flightId);
        }
        
        FlightAggregate flight = flightOpt.get();
        
        try {
            flight.reserveSeats(seats);
            flightDomainRepository.save(flight);
            
            log.info("Successfully reserved {} seats. Available seats now: {}", 
                    seats, flight.getAvailableSeats());
        } catch (InsufficientSeatsException e) {
            log.error("Failed to reserve seats: {}", e.getMessage());
            throw new com.airline.flight.exception.InsufficientSeatsException(
                    flightId, seats, flight.getAvailableSeats());
        } catch (InvalidFlightOperationException e) {
            log.error("Invalid flight operation: {}", e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        }
    }
    
    /**
     * 좌석 해제
     */
    @Transactional
    public void releaseSeats(String flightId, Integer seats) {
        log.info("Releasing {} seats for flight {}", seats, flightId);
        
        validateReleaseParameters(flightId, seats);
        
        FlightId domainFlightId = FlightId.of(flightId);
        Optional<FlightAggregate> flightOpt = flightDomainRepository.findById(domainFlightId);
        
        if (flightOpt.isEmpty()) {
            throw new FlightNotFoundException(flightId);
        }
        
        FlightAggregate flight = flightOpt.get();
        
        try {
            flight.releaseSeats(seats);
            flightDomainRepository.save(flight);
            
            log.info("Successfully released {} seats. Available seats now: {}", 
                    seats, flight.getAvailableSeats());
        } catch (InvalidFlightOperationException e) {
            log.error("Invalid flight operation: {}", e.getMessage());
            throw new InvalidRequestException(e.getMessage());
        }
    }
    
    
    // Validation Methods
    
    private void validateSearchParameters(String from, String to) {
        if (from == null || from.trim().isEmpty()) {
            throw new InvalidRequestException("Departure location cannot be null or empty");
        }
        if (to == null || to.trim().isEmpty()) {
            throw new InvalidRequestException("Arrival location cannot be null or empty");
        }
    }
    
    private void validateFlightId(String flightId) {
        if (flightId == null || flightId.trim().isEmpty()) {
            throw new InvalidRequestException("Flight ID cannot be null or empty");
        }
    }
    
    private void validateAvailabilityRequest(AvailabilityRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Availability request cannot be null");
        }
        if (request.getFlightId() == null || request.getFlightId().trim().isEmpty()) {
            throw new InvalidRequestException("Flight ID cannot be null or empty");
        }
        if (request.getRequestedSeats() == null || request.getRequestedSeats() <= 0) {
            throw new InvalidRequestException("Requested seats must be greater than 0");
        }
    }
    
    private void validateReservationParameters(String flightId, Integer seats) {
        validateFlightId(flightId);
        validateSeatCount(seats);
    }
    
    private void validateReleaseParameters(String flightId, Integer seats) {
        validateFlightId(flightId);
        validateSeatCount(seats);
    }
    
    private void validateSeatCount(Integer seats) {
        if (seats == null || seats <= 0) {
            throw new InvalidRequestException("Seat count must be greater than 0");
        }
    }
}