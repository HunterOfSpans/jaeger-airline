package com.airline.ticket.entity

import com.airline.ticket.dto.TicketStatus
import java.time.LocalDateTime

data class Ticket(
    val ticketId: String,
    var status: TicketStatus,
    val reservationId: String,
    val paymentId: String?,
    val flightId: String,
    val passengerName: String?,
    val passengerEmail: String?,
    val passengerPhone: String?,
    val passportNumber: String?,
    val seatNumber: String,
    val issuedAt: LocalDateTime,
    var message: String
)