package com.airline.ticket.dto

import java.time.LocalDateTime

data class TicketResponse(
    val ticketId: String,
    val status: TicketStatus,
    val reservationId: String,
    val paymentId: String,
    val flightId: String,
    val passengerInfo: PassengerInfo,
    val seatNumber: String,
    val issuedAt: LocalDateTime?,
    val message: String
)

enum class TicketStatus {
    PENDING, ISSUED, CANCELLED, USED, EXPIRED
}