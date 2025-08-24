package com.airline.ticket.dto

import java.math.BigDecimal

data class TicketRequest(
    val reservationId: String,
    val paymentId: String,
    val flightId: String,
    val passengerInfo: PassengerInfo,
    val seatNumber: String? = null
)

data class PassengerInfo(
    val name: String,
    val email: String,
    val phone: String,
    val passportNumber: String? = null
)