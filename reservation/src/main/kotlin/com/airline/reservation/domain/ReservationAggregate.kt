package com.airline.reservation.domain

import com.airline.reservation.common.DomainError
import com.airline.reservation.common.Result
import com.airline.reservation.dto.ReservationStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * 예약 도메인 모델 (불변성 강화)
 *
 * DDD의 Aggregate 패턴을 적용하여 예약의 비즈니스 로직을 캡슐화합니다.
 * 모든 상태는 불변이며, 상태 변경은 새로운 인스턴스를 반환합니다.
 */
data class ReservationAggregate private constructor(
    val reservationId: ReservationId,
    val status: ReservationStatus,
    val flightId: String,
    val passenger: PassengerInfo,
    val paymentId: String? = null,
    val ticketId: String? = null,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val seatNumber: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val message: String = ""
) {
    companion object {
        /**
         * 새로운 예약을 생성합니다.
         */
        fun create(
            flightId: String,
            passenger: PassengerInfo,
            paymentMethod: String
        ): Result<ReservationAggregate> {
            return when {
                flightId.isBlank() -> Result.failure(DomainError.ValidationError("Flight ID cannot be blank"))
                passenger.name.isBlank() -> Result.failure(DomainError.ValidationError("Passenger name cannot be blank"))
                passenger.email.isBlank() -> Result.failure(DomainError.ValidationError("Passenger email cannot be blank"))
                else -> Result.success(
                    ReservationAggregate(
                        reservationId = ReservationId.generate(),
                        status = ReservationStatus.PENDING,
                        flightId = flightId,
                        passenger = passenger,
                        message = "예약 생성됨"
                    )
                )
            }
        }
    }

    /**
     * 좌석을 예약합니다.
     */
    fun reserveSeat(): ReservationAggregate = copy(
        status = ReservationStatus.SEAT_RESERVED,
        message = "좌석 예약 완료"
    )

    /**
     * 결제를 완료합니다.
     */
    fun completePayment(paymentId: String, amount: BigDecimal): ReservationAggregate = copy(
        paymentId = paymentId,
        totalAmount = amount,
        status = ReservationStatus.PAYMENT_COMPLETED,
        message = "결제 완료"
    )

    /**
     * 항공권을 발급합니다.
     */
    fun issueTicket(ticketId: String, seatNumber: String): ReservationAggregate = copy(
        ticketId = ticketId,
        seatNumber = seatNumber,
        status = ReservationStatus.CONFIRMED,
        message = "예약 완료"
    )

    /**
     * 예약을 실패 처리합니다.
     */
    fun markAsFailed(reason: String): ReservationAggregate = copy(
        status = ReservationStatus.FAILED,
        message = reason
    )

    /**
     * 예약을 취소합니다.
     */
    fun cancel(): Result<ReservationAggregate> = when (status) {
        ReservationStatus.CONFIRMED -> Result.success(
            copy(
                status = ReservationStatus.CANCELLED,
                message = "예약 취소됨"
            )
        )
        else -> Result.failure(DomainError.ValidationError("Only confirmed reservations can be cancelled"))
    }

    /**
     * 취소 가능 여부를 확인합니다.
     */
    fun canBeCancelled(): Boolean = status == ReservationStatus.CONFIRMED
}

/**
 * 예약 ID 값 객체
 */
@JvmInline
value class ReservationId(val value: String) {
    companion object {
        fun generate(): ReservationId = ReservationId("RES-${UUID.randomUUID().toString().take(8)}")
        fun of(value: String): ReservationId = ReservationId(value)
    }

    override fun toString(): String = value
}

/**
 * 승객 정보 값 객체
 */
data class PassengerInfo(
    val name: String,
    val email: String,
    val phone: String,
    val passportNumber: String? = null
) {
    init {
        require(name.isNotBlank()) { "Passenger name cannot be blank" }
        require(email.isNotBlank()) { "Passenger email cannot be blank" }
        require(phone.isNotBlank()) { "Passenger phone cannot be blank" }
        require(email.contains("@")) { "Invalid email format" }
    }
}

/**
 * 보상 트랜잭션 정보
 */
data class CompensationInfo(
    val flightId: String,
    val paymentId: String? = null,
    val ticketId: String? = null,
    val seatsToRelease: Int = 1
)