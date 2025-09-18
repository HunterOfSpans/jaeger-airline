package com.airline.flight.domain.model;

import com.airline.flight.domain.event.FlightDomainEvent;
import com.airline.flight.domain.exception.InsufficientSeatsException;
import com.airline.flight.domain.exception.InvalidFlightOperationException;
import com.airline.flight.domain.valueobject.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Flight Aggregate Root - DDD 패턴 적용
 *
 * 항공편 도메인의 루트 엔티티로서 비즈니스 규칙과 불변성을 보장합니다.
 * 좌석 예약/해제 등의 도메인 로직을 캡슐화하고, 도메인 이벤트를 발행합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightAggregate {

    private FlightId flightId;
    private Airline airline;
    private Route route;
    private Schedule schedule;
    private PriceInfo priceInfo;
    private SeatInventory seatInventory;
    private Aircraft aircraft;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain Events
    private List<FlightDomainEvent> domainEvents = new ArrayList<>();

    // Factory method for creating new flight
    public static FlightAggregate create(
            String flightId,
            String airlineName,
            String departureCode,
            String arrivalCode,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            BigDecimal price,
            int totalSeats,
            String aircraftType) {

        FlightAggregate flight = new FlightAggregate();
        flight.flightId = FlightId.of(flightId);
        flight.airline = Airline.of(airlineName);
        flight.route = Route.of(
            Airport.of(departureCode),
            Airport.of(arrivalCode)
        );
        flight.schedule = Schedule.of(departureTime, arrivalTime);
        flight.priceInfo = PriceInfo.of(price);
        flight.seatInventory = SeatInventory.create(totalSeats);
        flight.aircraft = Aircraft.of(aircraftType);
        flight.createdAt = LocalDateTime.now();
        flight.updatedAt = LocalDateTime.now();

        // Domain Event 발행
        flight.addDomainEvent(FlightDomainEvent.FlightCreated.of(
            flightId, airlineName, departureCode, arrivalCode
        ));

        return flight;
    }

    // Factory method for reconstructing from persistence
    public static FlightAggregate reconstruct(
            String flightId,
            String airlineName,
            String departureCode,
            String arrivalCode,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            BigDecimal price,
            int totalSeats,
            int availableSeats,
            String aircraftType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        FlightAggregate flight = new FlightAggregate();
        flight.flightId = FlightId.of(flightId);
        flight.airline = Airline.of(airlineName);
        flight.route = Route.of(
            Airport.of(departureCode),
            Airport.of(arrivalCode)
        );
        flight.schedule = Schedule.of(departureTime, arrivalTime);
        flight.priceInfo = PriceInfo.of(price);
        flight.seatInventory = SeatInventory.reconstruct(totalSeats, availableSeats);
        flight.aircraft = Aircraft.of(aircraftType);
        flight.createdAt = createdAt;
        flight.updatedAt = updatedAt;

        return flight;
    }

    /**
     * 좌석 예약 - 도메인 비즈니스 로직
     */
    public void reserveSeats(int seatCount) {
        if (seatCount <= 0) {
            throw new InvalidFlightOperationException("예약 좌석 수는 0보다 커야 합니다");
        }

        if (!seatInventory.canReserve(seatCount)) {
            throw new InsufficientSeatsException(
                String.format("좌석 부족: 요청 %d석, 가용 %d석",
                    seatCount, seatInventory.getAvailableSeats())
            );
        }

        seatInventory = seatInventory.reserve(seatCount);
        updatedAt = LocalDateTime.now();

        // Domain Event 발행
        addDomainEvent(FlightDomainEvent.SeatsReserved.of(
            flightId.getValue(), seatCount, seatInventory.getAvailableSeats()
        ));
    }

    /**
     * 좌석 해제 - 도메인 비즈니스 로직
     */
    public void releaseSeats(int seatCount) {
        if (seatCount <= 0) {
            throw new InvalidFlightOperationException("해제 좌석 수는 0보다 커야 합니다");
        }

        seatInventory = seatInventory.release(seatCount);
        updatedAt = LocalDateTime.now();

        // Domain Event 발행
        addDomainEvent(FlightDomainEvent.SeatsReleased.of(
            flightId.getValue(), seatCount, seatInventory.getAvailableSeats()
        ));
    }

    /**
     * 가격 변경 - 도메인 비즈니스 로직
     */
    public void changePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidFlightOperationException("가격은 0보다 커야 합니다");
        }

        BigDecimal oldPrice = this.priceInfo.getAmount();
        this.priceInfo = PriceInfo.of(newPrice);
        this.updatedAt = LocalDateTime.now();

        // Domain Event 발행
        addDomainEvent(FlightDomainEvent.PriceChanged.of(
            flightId.getValue(), oldPrice, newPrice
        ));
    }

    /**
     * 좌석 가용성 확인
     */
    public boolean isAvailable(int requestedSeats) {
        return seatInventory.canReserve(requestedSeats);
    }

    /**
     * 출발 전인지 확인
     */
    public boolean isBeforeDeparture() {
        return schedule.isBeforeDeparture();
    }

    /**
     * 도메인 이벤트 추가
     */
    private void addDomainEvent(FlightDomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * 도메인 이벤트 조회 및 클리어
     */
    public List<FlightDomainEvent> pullDomainEvents() {
        List<FlightDomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    /**
     * 도메인 이벤트 조회 (읽기 전용)
     */
    public List<FlightDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }

    // 편의 메서드들
    public String getFlightNumber() {
        return flightId.getValue();
    }

    public String getAirlineName() {
        return airline.getName();
    }

    public String getDepartureCode() {
        return route.getDeparture().getCode();
    }

    public String getArrivalCode() {
        return route.getArrival().getCode();
    }

    public LocalDateTime getDepartureTime() {
        return schedule.getDepartureTime();
    }

    public LocalDateTime getArrivalTime() {
        return schedule.getArrivalTime();
    }

    public BigDecimal getPrice() {
        return priceInfo.getAmount();
    }

    public int getTotalSeats() {
        return seatInventory.getTotalSeats();
    }

    public int getAvailableSeats() {
        return seatInventory.getAvailableSeats();
    }

    public String getAircraftType() {
        return aircraft.getType();
    }
}