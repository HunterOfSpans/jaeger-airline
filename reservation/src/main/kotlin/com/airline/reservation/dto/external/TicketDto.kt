package com.airline.reservation.dto.external

import java.time.LocalDateTime

data class TicketRequest(
    val reservationId: String,
    val paymentId: String,
    val flightId: String,
    val passengerInfo: TicketPassengerInfo,
    val seatNumber: String? = null
)

data class TicketResponse(
    val ticketId: String,
    val status: TicketStatus,
    val reservationId: String,
    val paymentId: String,
    val flightId: String,
    val passengerInfo: TicketPassengerInfo,
    val seatNumber: String,
    val issuedAt: LocalDateTime,
    val message: String
)

data class TicketPassengerInfo(
    val name: String,
    val email: String,
    val phone: String,
    val passportNumber: String? = null
)

enum class TicketStatus {
    ISSUED, CANCELLED, USED, EXPIRED
}