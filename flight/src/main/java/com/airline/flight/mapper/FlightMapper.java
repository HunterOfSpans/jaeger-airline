package com.airline.flight.mapper;

import com.airline.flight.dto.FlightDto;
import com.airline.flight.entity.Flight;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FlightMapper {
    
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
    
    public List<FlightDto> toDtoList(List<Flight> flights) {
        return flights.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public List<Flight> toEntityList(List<FlightDto> dtos) {
        return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
}