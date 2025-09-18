package com.airline.flight.domain.repository;

import com.airline.flight.domain.model.FlightAggregate;
import com.airline.flight.domain.valueobject.FlightId;
import com.airline.flight.domain.valueobject.Route;

import java.util.List;
import java.util.Optional;

/**
 * Flight Domain Repository Interface
 *
 * DDD 패턴에 따른 도메인 리포지토리 인터페이스
 */
public interface FlightDomainRepository {

    /**
     * 항공편 저장
     */
    FlightAggregate save(FlightAggregate flight);

    /**
     * ID로 항공편 조회
     */
    Optional<FlightAggregate> findById(FlightId flightId);

    /**
     * 루트로 항공편 검색
     */
    List<FlightAggregate> findByRoute(Route route);

    /**
     * 출발지와 도착지로 항공편 검색
     */
    List<FlightAggregate> findByDepartureAndArrival(String departureCode, String arrivalCode);

    /**
     * 모든 항공편 조회
     */
    List<FlightAggregate> findAll();

    /**
     * 항공편 삭제
     */
    void delete(FlightId flightId);

    /**
     * 항공편 존재 여부 확인
     */
    boolean exists(FlightId flightId);
}