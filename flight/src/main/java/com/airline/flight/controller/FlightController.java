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

/**
 * 항공편 관리 REST API 컴트롤러
 * 
 * 항공편 조회, 좌석 가용성 확인, 좌석 예약/해제 등의 HTTP API를 제공합니다.
 * OpenTelemetry의 자동 계측을 통해 분산 추적을 지원하며,
 * RESTful API 설계 원칙을 따릅니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/v1/flights")
@RequiredArgsConstructor
public class FlightController {
    
    private final FlightService flightService;
    
    /**
     * 출발지와 도착지로 항공편을 검색합니다.
     * 
     * @param from 출발지 공항 코드 (선택사항)
     * @param to   도착지 공항 코드 (선택사항)
     * @param date 출발 날짜 (선택사항, 현재 미사용)
     * @return 검색 결과 항공편 목록
     */
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
    
    /**
     * 항공편 ID로 특정 항공편 정보를 조회합니다.
     * 
     * @param flightId 항공편 식별자
     * @return 항공편 정보, 존재하지 않으면 404 Not Found
     */
    @GetMapping("/{flightId}")
    public ResponseEntity<FlightDto> getFlightById(@PathVariable String flightId) {
        log.info("항공편 조회 요청: {}", flightId);
        FlightDto flight = flightService.getFlightById(flightId);
        
        if (flight == null) {
            log.warn("항공편을 찾을 수 없음: {}", flightId);
            return ResponseEntity.notFound().build();
        }
        
        log.info("항공편 조회 성공: {} ({}, {})", flightId, flight.getAirline(), flight.getPrice());
        return ResponseEntity.ok(flight);
    }
    
    /**
     * 항공편의 좌석 가용성을 확인합니다.
     * 
     * @param flightId 항공편 식별자
     * @param request  가용성 확인 요청 (요청 좌석 수 포함)
     * @return 가용성 확인 결과
     */
    @PostMapping("/{flightId}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable String flightId,
            @RequestBody AvailabilityRequest request) {
        
        request.setFlightId(flightId);
        AvailabilityResponse response = flightService.checkAvailability(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 항공편의 좌석을 예약합니다.
     * 
     * @param flightId 항공편 식별자
     * @param request  좌석 예약 요청 (예약 좌석 수 포함)
     * @return 예약 성공 시 200 OK, 실패 시 400 Bad Request
     */
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
    
    /**
     * 예약된 좌석을 해제합니다.
     * 
     * @param flightId 항공편 식별자
     * @param request  좌석 해제 요청 (해제 좌석 수 포함)
     * @return 200 OK
     */
    @PostMapping("/{flightId}/release")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable String flightId,
            @RequestBody AvailabilityRequest request) {
        
        flightService.releaseSeats(flightId, request.getRequestedSeats());
        return ResponseEntity.ok().build();
    }
}