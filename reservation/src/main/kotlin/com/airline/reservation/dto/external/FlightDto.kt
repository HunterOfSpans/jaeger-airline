package com.airline.reservation.dto.external

import java.math.BigDecimal
import java.time.LocalDateTime

data class FlightDto(
    val flightId: String,
    val airline: String,
    val departure: String,
    val arrival: String,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val price: BigDecimal,
    val availableSeats: Int,
    val aircraft: String
)

data class AvailabilityRequest(
    val flightId: String? = null,
    val requestedSeats: Int
)

data class AvailabilityResponse(
    val available: Boolean,
    val flightId: String,
    val availableSeats: Int,
    val message: String
)