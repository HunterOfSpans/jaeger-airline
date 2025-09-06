package com.airline.reservation.mapper

import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.dto.ReservationStatus
import com.airline.reservation.entity.Reservation
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 예약 데이터 변환 매퍼
 * 
 * Reservation 엔티티와 ReservationResponse DTO 간의 변환을 담당합니다.
 * ReservationRequest로부터 Reservation 엔티티를 생성하고,
 * Reservation 엔티티를 ReservationResponse로 변환하는 기능을 제공합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Component
class ReservationMapper {
    
    /**
     * ReservationRequest로부터 Reservation 엔티티를 생성합니다.
     * 
     * @param request       예약 요청 DTO
     * @param reservationId 생성된 예약 식별자
     * @return 생성된 Reservation 엔티티 (초기 상태는 PENDING)
     */
    fun toEntity(request: ReservationRequest, reservationId: String): Reservation {
        return Reservation(
            reservationId = reservationId,
            status = ReservationStatus.PENDING,
            flightId = request.flightId,
            passengerName = request.passengerInfo.name,
            passengerEmail = request.passengerInfo.email,
            passengerPhone = request.passengerInfo.phone,
            passportNumber = request.passengerInfo.passportNumber,
            paymentId = null,
            ticketId = null,
            totalAmount = BigDecimal.ZERO,
            seatNumber = null,
            createdAt = LocalDateTime.now(),
            message = "Reservation initiated"
        )
    }
    
    /**
     * Reservation 엔티티를 ReservationResponse DTO로 변환합니다.
     * 
     * @param reservation 변환할 Reservation 엔티티
     * @return 변환된 ReservationResponse DTO
     */
    fun toResponse(reservation: Reservation): ReservationResponse {
        return ReservationResponse(
            reservationId = reservation.reservationId,
            status = reservation.status,
            flightId = reservation.flightId,
            passengerInfo = com.airline.reservation.dto.PassengerInfo(
                name = reservation.passengerName,
                email = reservation.passengerEmail,
                phone = reservation.passengerPhone ?: "",
                passportNumber = reservation.passportNumber ?: ""
            ),
            paymentId = reservation.paymentId,
            ticketId = reservation.ticketId,
            totalAmount = reservation.totalAmount,
            seatNumber = reservation.seatNumber,
            createdAt = reservation.createdAt,
            message = reservation.message
        )
    }
    
    /**
     * Reservation 엔티티 목록을 ReservationResponse DTO 목록으로 변환합니다.
     * 
     * @param reservations 변환할 Reservation 엔티티 목록
     * @return 변환된 ReservationResponse DTO 목록
     */
    fun toResponseList(reservations: List<Reservation>): List<ReservationResponse> {
        return reservations.map { toResponse(it) }
    }
}