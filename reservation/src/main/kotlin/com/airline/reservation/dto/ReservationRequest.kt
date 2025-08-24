package com.airline.reservation.dto

import java.math.BigDecimal

data class ReservationRequest(
    val flightId: String,
    val passengerInfo: PassengerInfo,
    val seatPreference: String?,
    val paymentMethod: String
)

data class PassengerInfo(
    val name: String,
    val email: String,
    val phone: String,
    val passportNumber: String? = null
)