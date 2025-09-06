package com.airline.flight.mapper;

import com.airline.flight.dto.FlightDto;
import com.airline.flight.entity.Flight;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 항공편 데이터 변환 매퍼
 * 
 * Flight 엔티티와 FlightDto 간의 양방향 변환을 담당합니다.
 * 비즈니스 로직에서 데이터 변환 로직을 분리하여 깨끗한 아키텍처를 유지합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Component
public class FlightMapper {
    
    /**
     * Flight 엔티티를 FlightDto로 변환합니다.
     * 
     * @param flight 변환할 항공편 엔티티
     * @return 변환된 FlightDto, 입력이 null이면 null 반환
     */
    public FlightDto toDto(Flight flight) {
        if (flight == null) {
            return null;
        }
        
        return new FlightDto(
            flight.getFlightId(),
            flight.getAirline(),
            flight.getDeparture(),
            flight.getArrival(),
            flight.getDepartureTime(),
            flight.getArrivalTime(),
            flight.getPrice(),
            flight.getAvailableSeats(),
            flight.getAircraft()
        );
    }
    
    /**
     * FlightDto를 Flight 엔티티로 변환합니다.
     * 
     * @param dto 변환할 항공편 DTO
     * @return 변환된 Flight 엔티티, 입력이 null이면 null 반환
     */
    public Flight toEntity(FlightDto dto) {
        if (dto == null) {
            return null;
        }
        
        return new Flight(
            dto.getFlightId(),
            dto.getAirline(),
            dto.getDeparture(),
            dto.getArrival(),
            dto.getDepartureTime(),
            dto.getArrivalTime(),
            dto.getPrice(),
            dto.getAvailableSeats(),
            dto.getAircraft()
        );
    }
    
    /**
     * Flight 엔티티 목록을 FlightDto 목록으로 변환합니다.
     * 
     * @param flights 변환할 항공편 엔티티 목록
     * @return 변환된 FlightDto 목록
     */
    public List<FlightDto> toDtoList(List<Flight> flights) {
        return flights.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * FlightDto 목록을 Flight 엔티티 목록으로 변환합니다.
     * 
     * @param dtos 변환할 항공편 DTO 목록
     * @return 변환된 Flight 엔티티 목록
     */
    public List<Flight> toEntityList(List<FlightDto> dtos) {
        return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
}