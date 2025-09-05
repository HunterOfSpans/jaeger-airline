package com.airline.reservation.mapper

import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.dto.ReservationStatus
import com.airline.reservation.entity.Reservation
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

@Component
class ReservationMapper {
    
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
    
    fun toResponseList(reservations: List<Reservation>): List<ReservationResponse> {
        return reservations.map { toResponse(it) }
    }
}