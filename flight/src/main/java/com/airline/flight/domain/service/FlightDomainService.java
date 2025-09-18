package com.airline.flight.domain.service;

import com.airline.flight.domain.model.FlightAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Flight Domain Service
 *
 * 복잡한 비즈니스 로직과 도메인 규칙을 처리하는 도메인 서비스
 * (사용하지 않는 메서드들 제거 완료)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlightDomainService {

    /**
     * 좌석 예약 우선순위 결정
     */
    public FlightAggregate selectBestFlightForReservation(List<FlightAggregate> availableFlights, int requestedSeats) {
        return availableFlights.stream()
            .filter(flight -> flight.isAvailable(requestedSeats))
            .filter(FlightAggregate::isBeforeDeparture)
            .min((f1, f2) -> {
                // 가격이 저렴한 순서
                int priceComparison = f1.getPrice().compareTo(f2.getPrice());
                if (priceComparison != 0) {
                    return priceComparison;
                }
                // 가격이 같으면 여유 좌석이 많은 순서
                return Integer.compare(f2.getAvailableSeats(), f1.getAvailableSeats());
            })
            .orElse(null);
    }
}