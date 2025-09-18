package com.airline.reservation.mapper

import com.airline.reservation.domain.ReservationAggregate
import com.airline.reservation.domain.ReservationId
import com.airline.reservation.dto.ReservationStatus
import com.airline.reservation.entity.Reservation
import java.time.LocalDateTime

/**
 * 도메인 모델과 엔티티 간 매핑 확장 함수
 */

fun ReservationMapper.toEntity(aggregate: ReservationAggregate): Reservation = Reservation(
    reservationId = aggregate.reservationId.value,
    status = mapStatus(aggregate.status),
    flightId = aggregate.flightId,
    passengerName = aggregate.passenger.name,
    passengerEmail = aggregate.passenger.email,
    passengerPhone = aggregate.passenger.phone,
    passportNumber = aggregate.passenger.passportNumber,
    paymentId = aggregate.paymentId,
    ticketId = aggregate.ticketId,
    totalAmount = aggregate.totalAmount,
    seatNumber = aggregate.seatNumber,
    createdAt = aggregate.createdAt,
    message = aggregate.message
)

fun ReservationMapper.toDomain(entity: Reservation): ReservationAggregate =
    ReservationAggregate::class.java.getDeclaredConstructor(
        ReservationId::class.java,
        ReservationStatus::class.java,
        String::class.java,
        com.airline.reservation.domain.PassengerInfo::class.java,
        String::class.java,
        String::class.java,
        java.math.BigDecimal::class.java,
        String::class.java,
        LocalDateTime::class.java,
        String::class.java
    ).apply { isAccessible = true }.newInstance(
        ReservationId.of(entity.reservationId),
        entity.status,
        entity.flightId,
        com.airline.reservation.domain.PassengerInfo(
            name = entity.passengerName,
            email = entity.passengerEmail,
            phone = entity.passengerPhone ?: "",
            passportNumber = entity.passportNumber
        ),
        entity.paymentId,
        entity.ticketId,
        entity.totalAmount,
        entity.seatNumber,
        entity.createdAt,
        entity.message
    )

private fun mapStatus(status: ReservationStatus): ReservationStatus = when (status) {
    ReservationStatus.PENDING -> ReservationStatus.PENDING
    ReservationStatus.SEAT_RESERVED -> ReservationStatus.SEAT_RESERVED
    ReservationStatus.PAYMENT_COMPLETED -> ReservationStatus.PAYMENT_COMPLETED
    ReservationStatus.CONFIRMED -> ReservationStatus.CONFIRMED
    ReservationStatus.CANCELLED -> ReservationStatus.CANCELLED
    ReservationStatus.FAILED -> ReservationStatus.FAILED
}