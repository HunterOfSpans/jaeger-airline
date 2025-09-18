package com.airline.flight.infrastructure.repository;

import com.airline.flight.domain.model.FlightAggregate;
import com.airline.flight.domain.repository.FlightDomainRepository;
import com.airline.flight.domain.valueobject.FlightId;
import com.airline.flight.domain.valueobject.Route;
import com.airline.flight.entity.Flight;
import com.airline.flight.mapper.FlightMapper;
import com.airline.flight.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Flight Domain Repository Implementation
 *
 * 도메인 리포지토리와 인프라스트럭처 리포지토리 간의 어댑터
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FlightDomainRepositoryImpl implements FlightDomainRepository {

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;

    @Override
    public FlightAggregate save(FlightAggregate flightAggregate) {
        log.debug("Saving flight aggregate: {}", flightAggregate.getFlightNumber());

        Flight entity = flightMapper.toEntity(flightAggregate);
        Flight savedEntity = flightRepository.save(entity);

        return flightMapper.toDomainAggregate(savedEntity);
    }

    @Override
    public Optional<FlightAggregate> findById(FlightId flightId) {
        log.debug("Finding flight by ID: {}", flightId.getValue());

        return flightRepository.findById(flightId.getValue())
                .map(flightMapper::toDomainAggregate);
    }

    @Override
    public List<FlightAggregate> findByRoute(Route route) {
        log.debug("Finding flights by route: {} -> {}",
                route.getDeparture().getCode(), route.getArrival().getCode());

        return flightRepository.findByDepartureAndArrival(
                        route.getDeparture().getCode(),
                        route.getArrival().getCode())
                .stream()
                .map(flightMapper::toDomainAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightAggregate> findByDepartureAndArrival(String departureCode, String arrivalCode) {
        log.debug("Finding flights from {} to {}", departureCode, arrivalCode);

        return flightRepository.findByDepartureAndArrival(departureCode, arrivalCode)
                .stream()
                .map(flightMapper::toDomainAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightAggregate> findAll() {
        log.debug("Finding all flights");

        return flightRepository.findAll()
                .stream()
                .map(flightMapper::toDomainAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(FlightId flightId) {
        log.debug("Deleting flight: {}", flightId.getValue());
        flightRepository.deleteById(flightId.getValue());
    }

    @Override
    public boolean exists(FlightId flightId) {
        log.debug("Checking if flight exists: {}", flightId.getValue());
        return flightRepository.existsById(flightId.getValue());
    }
}