package com.airline.reservation.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ReservationResponse(
    val reservationId: String,
    val status: ReservationStatus,
    val flightId: String,
    val passengerInfo: PassengerInfo,
    val paymentId: String?,
    val ticketId: String?,
    val totalAmount: BigDecimal,
    val seatNumber: String?,
    val createdAt: LocalDateTime,
    val message: String
)

enum class ReservationStatus {
    PENDING, SEAT_RESERVED, PAYMENT_COMPLETED, CONFIRMED, CANCELLED, FAILED
}