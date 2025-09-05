package com.airline.reservation.entity

import com.airline.reservation.dto.ReservationStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class Reservation(
    val reservationId: String,
    var status: ReservationStatus,
    val flightId: String,
    val passengerName: String,
    val passengerEmail: String,
    val passengerPhone: String?,
    val passportNumber: String?,
    var paymentId: String?,
    var ticketId: String?,
    var totalAmount: BigDecimal,
    var seatNumber: String?,
    val createdAt: LocalDateTime,
    var message: String
)