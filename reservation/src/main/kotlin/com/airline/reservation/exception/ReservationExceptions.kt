package com.airline.reservation.exception

/**
 * 예약 관련 예외 클래스들
 * 
 * @author Claude Code
 * @since 1.0
 */

sealed class ReservationException(message: String) : RuntimeException(message)

class ReservationNotFoundException(reservationId: String) : ReservationException(
    "Reservation not found with ID: $reservationId"
)

class ReservationProcessingException(message: String) : ReservationException(
    "Reservation processing failed: $message"
)

class InvalidReservationRequestException(message: String) : ReservationException(
    "Invalid reservation request: $message"
)

class FlightServiceException(message: String) : ReservationException(
    "Flight service error: $message"
)

class PaymentServiceException(message: String) : ReservationException(
    "Payment service error: $message"
)

class TicketServiceException(message: String) : ReservationException(
    "Ticket service error: $message"
)

class CompensationException(message: String) : ReservationException(
    "Compensation transaction failed: $message"
)