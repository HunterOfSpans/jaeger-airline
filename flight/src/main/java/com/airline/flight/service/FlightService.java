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
            log.warn("좌석 해제 대상 항공편을 찾을 수 없음: {}", flightId);
        }
    }
    
    /**
     * 출발지와 도착지로 항공편을 검색합니다.
     */
    private List<Flight> findFlightsByRoute(String from, String to) {
        return flightRepository.findByDepartureAndArrival(from, to);
    }
    
    /**
     * 항공편을 찾을 수 없을 때의 응답을 생성합니다.
     */
    private AvailabilityResponse createNotFoundResponse(String flightId) {
        return new AvailabilityResponse(false, flightId, 0, "항공편을 찾을 수 없습니다");
    }
    
    /**
     * 가용성 확인 응답을 생성합니다.
     */
    private AvailabilityResponse createAvailabilityResponse(Flight flight, AvailabilityRequest request) {
        boolean available = flight.getAvailableSeats() >= request.getRequestedSeats();
        String message = available ? "좌석 예약 가능" : "가용 좌석 부족";
        
        return new AvailabilityResponse(available, request.getFlightId(), 
                                      flight.getAvailableSeats(), message);
    }
    
    /**
     * 좌석 예약 처리 로직을 수행합니다.
     */
    private boolean processReservation(Flight flight, Integer seats) {
        if (flight.getAvailableSeats() >= seats) {
            flight.setAvailableSeats(flight.getAvailableSeats() - seats);
            flightRepository.save(flight);
            log.info("{}석 예약 성공. 남은 좌석: {}석", seats, flight.getAvailableSeats());
            return true;
        }
        
        log.warn("좌석 부족으로 예약 실패. 요청: {}석, 가용: {}석", seats, flight.getAvailableSeats());
        return false;
    }
    
    /**
     * 좌석 해제 처리 로직을 수행합니다.
     */
    private void processRelease(Flight flight, Integer seats) {
        flight.setAvailableSeats(flight.getAvailableSeats() + seats);
        flightRepository.save(flight);
        log.info("{}석 해제 성공. 현재 가용 좌석: {}석", seats, flight.getAvailableSeats());
    }
}