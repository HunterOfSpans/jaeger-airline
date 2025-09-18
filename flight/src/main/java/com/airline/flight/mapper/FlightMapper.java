package com.airline.flight.mapper;

import com.airline.flight.domain.model.FlightAggregate;
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

    /**
     * FlightAggregate 도메인 모델을 Flight 엔티티로 변환합니다.
     *
     * @param aggregate 변환할 항공편 도메인 모델
     * @return 변환된 Flight 엔티티
     */
    public Flight toEntity(FlightAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        Flight flight = new Flight();
        flight.setFlightId(aggregate.getFlightNumber());
        flight.setAirline(aggregate.getAirlineName());
        flight.setDeparture(aggregate.getDepartureCode());
        flight.setArrival(aggregate.getArrivalCode());
        flight.setDepartureTime(aggregate.getDepartureTime());
        flight.setArrivalTime(aggregate.getArrivalTime());
        flight.setPrice(aggregate.getPrice());
        flight.setTotalSeats(aggregate.getTotalSeats());
        flight.setAvailableSeats(aggregate.getAvailableSeats());
        flight.setAircraft(aggregate.getAircraftType());

        return flight;
    }

    /**
     * Flight 엔티티를 FlightAggregate 도메인 모델로 변환합니다.
     *
     * @param flight 변환할 항공편 엔티티
     * @return 변환된 FlightAggregate 도메인 모델
     */
    public FlightAggregate toDomainAggregate(Flight flight) {
        if (flight == null) {
            return null;
        }

        return FlightAggregate.reconstruct(
            flight.getFlightId(),
            flight.getAirline(),
            flight.getDeparture(),
            flight.getArrival(),
            flight.getDepartureTime(),
            flight.getArrivalTime(),
            flight.getPrice(),
            flight.getTotalSeats(),
            flight.getAvailableSeats(),
            flight.getAircraft(),
            null, // createdAt - 추후 추가 가능
            null  // updatedAt - 추후 추가 가능
        );
    }

    /**
     * FlightAggregate를 FlightDto로 변환합니다.
     *
     * @param aggregate 변환할 항공편 도메인 모델
     * @return 변환된 FlightDto
     */
    public FlightDto toDto(FlightAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        return new FlightDto(
            aggregate.getFlightNumber(),
            aggregate.getAirlineName(),
            aggregate.getDepartureCode(),
            aggregate.getArrivalCode(),
            aggregate.getDepartureTime(),
            aggregate.getArrivalTime(),
            aggregate.getPrice(),
            aggregate.getAvailableSeats(),
            aggregate.getAircraftType()
        );
    }
}