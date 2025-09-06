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

/**
 * 항공편 데이터 접근 계층
 * 
 * 항공편 정보의 CRUD 작업과 비즈니스 로직에 특화된 조회 기능을 제공합니다.
 * ConcurrentHashMap을 사용한 인메모리 저장소로 구현되어 있으며,
 * 실제 프로덕션에서는 JPA Repository로 교체 가능한 구조입니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
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
    
    /**
     * 항공편 정보를 저장하거나 업데이트합니다.
     * 
     * @param flight 저장할 항공편 엔티티
     * @return 저장된 항공편 엔티티
     */
    public Flight save(Flight flight) {
        flights.put(flight.getFlightId(), flight);
        return flight;
    }
    
    /**
     * 항공편 ID로 특정 항공편을 조회합니다.
     * 
     * @param flightId 항공편 식별자
     * @return 항공편 엔티티를 담은 Optional, 존재하지 않으면 빈 Optional
     */
    public Optional<Flight> findById(String flightId) {
        return Optional.ofNullable(flights.get(flightId));
    }
    
    /**
     * 모든 항공편을 조회합니다.
     * 
     * @return 전체 항공편 목록
     */
    public List<Flight> findAll() {
        return flights.values().stream().collect(Collectors.toList());
    }
    
    /**
     * 출발지와 도착지로 항공편을 검색합니다.
     * 
     * @param departure 출발지 공항 코드
     * @param arrival   도착지 공항 코드
     * @return 검색 조건에 맞는 항공편 목록
     */
    public List<Flight> findByDepartureAndArrival(String departure, String arrival) {
        return flights.values().stream()
            .filter(flight -> flight.getDeparture().equalsIgnoreCase(departure) && 
                             flight.getArrival().equalsIgnoreCase(arrival))
            .collect(Collectors.toList());
    }
    
    /**
     * 특정 항공편이 존재하는지 확인합니다.
     * 
     * @param flightId 항공편 식별자
     * @return 존재 여부
     */
    public boolean existsById(String flightId) {
        return flights.containsKey(flightId);
    }
    
    /**
     * 특정 항공편을 삭제합니다.
     * 
     * @param flightId 삭제할 항공편 식별자
     */
    public void deleteById(String flightId) {
        flights.remove(flightId);
    }
    
    /**
     * 저장된 항공편 총 개수를 반환합니다.
     * 
     * @return 전체 항공편 수
     */
    public long count() {
        return flights.size();
    }
}