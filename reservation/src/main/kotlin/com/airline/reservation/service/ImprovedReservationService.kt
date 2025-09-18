package com.airline.reservation.service

import com.airline.reservation.client.FlightClient
import com.airline.reservation.client.PaymentClient
import com.airline.reservation.client.TicketClient
import com.airline.reservation.common.DomainError
import com.airline.reservation.common.Result
import com.airline.reservation.domain.CompensationInfo
import com.airline.reservation.domain.PassengerInfo
import com.airline.reservation.domain.ReservationAggregate
import com.airline.reservation.domain.ReservationId
import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.dto.ReservationStatus
import com.airline.reservation.dto.external.*
import com.airline.reservation.mapper.ReservationMapper
import com.airline.reservation.repository.ReservationRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

/**
 * 개선된 예약 서비스
 *
 * 개선 사항:
 * 1. Result 타입으로 예외 처리 개선
 * 2. 불변 도메인 모델 활용
 * 3. 함수형 프로그래밍 스타일 적용
 * 4. 책임 분리 및 메서드 추출
 * 5. Kotlin 관용구 활용
 */
@Service
class ImprovedReservationService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val flightClient: FlightClient,
    private val paymentClient: PaymentClient,
    private val ticketClient: TicketClient,
    private val reservationRepository: ReservationRepository,
    private val reservationMapper: ReservationMapper,
    private val flightService: FlightService = FlightService(flightClient),
    private val paymentService: PaymentService = PaymentService(paymentClient),
    private val ticketService: TicketService = TicketService(ticketClient),
    private val compensationService: CompensationService = CompensationService(
        flightClient, paymentClient, ticketClient
    )
) {
    private val logger = LoggerFactory.getLogger(ImprovedReservationService::class.java)

    @CircuitBreaker(name = "reservation", fallbackMethod = "createReservationFallback")
    fun createReservation(request: ReservationRequest): Result<ReservationResponse> {
        logger.info("예약 프로세스 시작: 항공편 {}", request.flightId)

        return ReservationAggregate.create(
            flightId = request.flightId,
            passenger = PassengerInfo(
                name = request.passengerInfo.name,
                email = request.passengerInfo.email,
                phone = request.passengerInfo.phone,
                passportNumber = request.passengerInfo.passportNumber
            ),
            paymentMethod = request.paymentMethod
        ).flatMap { reservation ->
            processReservationWorkflow(reservation, request)
        }.onSuccess { response ->
            logger.info("예약 완료: {}", response.reservationId)
        }.onFailure { error ->
            logger.error("예약 실패: {}", error.message)
        }
    }

    fun getReservationById(reservationId: String): Result<ReservationResponse> {
        return Result.runCatching {
            require(reservationId.isNotBlank()) { "Reservation ID cannot be blank" }

            reservationRepository.findById(reservationId)
                ?.let { reservationMapper.toResponse(it) }
                ?: throw NoSuchElementException("Reservation not found: $reservationId")
        }
    }

    fun cancelReservation(reservationId: String): Result<ReservationResponse> {
        return getReservationById(reservationId)
            .flatMap { response ->
                if (response.status == ReservationStatus.CONFIRMED) {
                    executeCancellation(response)
                } else {
                    Result.failure(DomainError.ValidationError("Only confirmed reservations can be cancelled"))
                }
            }
    }

    private fun processReservationWorkflow(
        reservation: ReservationAggregate,
        request: ReservationRequest
    ): Result<ReservationResponse> {
        return validateAndReserveFlight(reservation.flightId)
            .flatMap { flight ->
                val updatedReservation = reservation.reserveSeat()
                processPayment(updatedReservation, flight, request)
            }
            .flatMap { (updatedReservation, payment) ->
                issueTicket(updatedReservation, payment, request)
            }
            .flatMap { (finalReservation, _) ->
                saveAndPublishReservation(finalReservation)
            }
            .recoverWith { error ->
                handleReservationFailure(reservation, error)
            }
    }

    private fun validateAndReserveFlight(flightId: String): Result<FlightDto> {
        return flightService.validateFlightAvailability(flightId, 1)
            .flatMap { flight ->
                flightService.reserveSeats(flightId, 1)
                    .map { flight }
            }
    }

    private fun processPayment(
        reservation: ReservationAggregate,
        flight: FlightDto,
        request: ReservationRequest
    ): Result<Pair<ReservationAggregate, PaymentResponse>> {
        val paymentRequest = createPaymentRequest(reservation, flight, request)

        return paymentService.processPayment(paymentRequest)
            .map { payment ->
                val updatedReservation = reservation.completePayment(payment.paymentId, flight.price)
                updatedReservation to payment
            }
    }

    private fun issueTicket(
        reservation: ReservationAggregate,
        payment: PaymentResponse,
        request: ReservationRequest
    ): Result<Pair<ReservationAggregate, TicketResponse>> {
        val ticketRequest = createTicketRequest(reservation, payment, request)

        return ticketService.issueTicket(ticketRequest)
            .map { ticket ->
                val finalReservation = reservation.issueTicket(ticket.ticketId, ticket.seatNumber)
                finalReservation to ticket
            }
    }

    private fun saveAndPublishReservation(reservation: ReservationAggregate): Result<ReservationResponse> {
        return Result.runCatching {
            // 임시로 기존 entity로 변환
            val tempRequest = com.airline.reservation.dto.ReservationRequest(
                flightId = reservation.flightId,
                passengerInfo = com.airline.reservation.dto.PassengerInfo(
                    name = reservation.passenger.name,
                    email = reservation.passenger.email,
                    phone = reservation.passenger.phone,
                    passportNumber = reservation.passenger.passportNumber
                ),
                seatPreference = null,
                paymentMethod = "CARD"
            )
            val entity = reservationMapper.toEntity(tempRequest, reservation.reservationId.value)

            // 상태 업데이트
            entity.status = ReservationStatus.CONFIRMED
            entity.paymentId = reservation.paymentId
            entity.ticketId = reservation.ticketId
            entity.totalAmount = reservation.totalAmount
            entity.seatNumber = reservation.seatNumber
            entity.message = reservation.message

            val savedEntity = reservationRepository.save(entity)

            // 이벤트 발행
            kafkaTemplate.send("reservation.created", "Reservation completed: ${reservation.reservationId}")

            reservationMapper.toResponse(savedEntity)
        }
    }

    private fun handleReservationFailure(
        reservation: ReservationAggregate,
        error: DomainError
    ): Result<ReservationResponse> {
        logger.error("예약 실패 - 보상 트랜잭션 실행: {}", reservation.reservationId)

        return compensationService.executeCompensation(
            CompensationInfo(
                flightId = reservation.flightId,
                paymentId = reservation.paymentId,
                ticketId = reservation.ticketId
            )
        ).map {
            val failedReservation = reservation.markAsFailed(error.message)
            // 임시로 기존 entity로 변환
            val tempRequest = com.airline.reservation.dto.ReservationRequest(
                flightId = failedReservation.flightId,
                passengerInfo = com.airline.reservation.dto.PassengerInfo(
                    name = failedReservation.passenger.name,
                    email = failedReservation.passenger.email,
                    phone = failedReservation.passenger.phone,
                    passportNumber = failedReservation.passenger.passportNumber
                ),
                seatPreference = null,
                paymentMethod = "CARD"
            )
            val entity = reservationMapper.toEntity(tempRequest, failedReservation.reservationId.value)
            entity.status = ReservationStatus.FAILED
            entity.message = failedReservation.message
            val savedEntity = reservationRepository.save(entity)
            reservationMapper.toResponse(savedEntity)
        }
    }

    private fun executeCancellation(response: ReservationResponse): Result<ReservationResponse> {
        return compensationService.executeCompensation(
            CompensationInfo(
                flightId = response.flightId,
                paymentId = response.paymentId,
                ticketId = response.ticketId
            )
        ).flatMap {
            Result.runCatching {
                val entity = reservationRepository.findById(response.reservationId)!!
                entity.status = ReservationStatus.CANCELLED
                entity.message = "예약 취소됨"
                reservationMapper.toResponse(reservationRepository.save(entity))
            }
        }
    }

    private fun createPaymentRequest(
        reservation: ReservationAggregate,
        flight: FlightDto,
        request: ReservationRequest
    ): PaymentRequest = PaymentRequest(
        reservationId = reservation.reservationId.value,
        amount = flight.price,
        paymentMethod = request.paymentMethod,
        customerInfo = CustomerInfo(
            name = reservation.passenger.name,
            email = reservation.passenger.email
        )
    )

    private fun createTicketRequest(
        reservation: ReservationAggregate,
        payment: PaymentResponse,
        request: ReservationRequest
    ): TicketRequest = TicketRequest(
        reservationId = reservation.reservationId.value,
        paymentId = payment.paymentId,
        flightId = request.flightId,
        passengerInfo = TicketPassengerInfo(
            name = reservation.passenger.name,
            email = reservation.passenger.email,
            phone = reservation.passenger.phone,
            passportNumber = reservation.passenger.passportNumber
        )
    )

    fun createReservationFallback(request: ReservationRequest, ex: Exception): Result<ReservationResponse> {
        logger.error("Circuit Breaker 활성화 - 예약 서비스 일시 중단", ex)
        return Result.failure(DomainError.SystemError("서비스 일시 중단. 잠시 후 다시 시도해주세요.", ex))
    }
}

/**
 * 항공편 관련 서비스
 */
class FlightService(private val flightClient: FlightClient) {
    private val logger = LoggerFactory.getLogger(FlightService::class.java)

    fun validateFlightAvailability(flightId: String, requestedSeats: Int): Result<FlightDto> {
        return Result.runCatching {
            val flight = flightClient.getFlightById(flightId)
                ?: throw NoSuchElementException("Flight not found: $flightId")

            val availability = flightClient.checkAvailability(
                flightId, AvailabilityRequest(flightId = flightId, requestedSeats = requestedSeats)
            )

            if (!availability.available) {
                throw IllegalStateException("No available seats for flight: $flightId")
            }

            flight
        }.mapFailure { ex ->
            when {
                ex is NoSuchElementException -> DomainError.FlightNotFound(flightId)
                ex.message?.contains("No available seats") == true -> DomainError.NoAvailableSeats(flightId)
                else -> DomainError.SystemError("Flight validation failed", ex)
            }
        }
    }

    fun reserveSeats(flightId: String, seats: Int): Result<Unit> {
        return Result.runCatching {
            flightClient.reserveSeats(flightId, AvailabilityRequest(flightId = flightId, requestedSeats = seats))
            logger.info("좌석 예약 완료: {} ({}석)", flightId, seats)
        }
    }
}

/**
 * 결제 관련 서비스
 */
class PaymentService(private val paymentClient: PaymentClient) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    fun processPayment(request: PaymentRequest): Result<PaymentResponse> {
        return Result.runCatching {
            val response = paymentClient.processPayment(request)

            if (response.status != PaymentStatus.SUCCESS) {
                throw IllegalStateException("Payment declined: ${response.message}")
            }

            logger.info("결제 처리 완료: {}", response.paymentId)
            response
        }.mapFailure { ex ->
            DomainError.PaymentDeclined("Payment processing failed: ${ex.message}", ex)
        }
    }
}

/**
 * 항공권 관련 서비스
 */
class TicketService(private val ticketClient: TicketClient) {
    private val logger = LoggerFactory.getLogger(TicketService::class.java)

    fun issueTicket(request: TicketRequest): Result<TicketResponse> {
        return Result.runCatching {
            val response = ticketClient.issueTicket(request)
            logger.info("항공권 발급 완료: {}", response.ticketId)
            response
        }.mapFailure { ex ->
            DomainError.TicketIssuanceFailed("Ticket issuance failed: ${ex.message}", ex)
        }
    }
}

/**
 * 보상 트랜잭션 서비스
 */
class CompensationService(
    private val flightClient: FlightClient,
    private val paymentClient: PaymentClient,
    private val ticketClient: TicketClient
) {
    private val logger = LoggerFactory.getLogger(CompensationService::class.java)

    fun executeCompensation(info: CompensationInfo): Result<Unit> {
        logger.info("보상 트랜잭션 실행: {}", info)

        val results = listOfNotNull(
            info.ticketId?.let { cancelTicket(it) },
            info.paymentId?.let { cancelPayment(it) },
            releaseSeats(info.flightId, info.seatsToRelease)
        )

        return if (results.all { it.isSuccess() }) {
            Result.success(Unit)
        } else {
            val failures = results.filter { it.isFailure() }
            logger.error("보상 트랜잭션 일부 실패: {}", failures)
            Result.failure(DomainError.SystemError("Compensation partially failed"))
        }
    }

    private fun cancelTicket(ticketId: String): Result<Unit> = Result.runCatching {
        ticketClient.cancelTicket(ticketId)
        logger.info("항공권 취소 완료: {}", ticketId)
    }

    private fun cancelPayment(paymentId: String): Result<Unit> = Result.runCatching {
        paymentClient.cancelPayment(paymentId)
        logger.info("결제 취소 완료: {}", paymentId)
    }

    private fun releaseSeats(flightId: String, seats: Int): Result<Unit> = Result.runCatching {
        flightClient.releaseSeats(flightId, AvailabilityRequest(flightId = flightId, requestedSeats = seats))
        logger.info("좌석 해제 완료: {} ({}석)", flightId, seats)
    }
}

// Result 확장 함수들
private inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> DomainError): Result<T> = when (this) {
    is Result.Success -> this
    is Result.Failure -> this
    else -> when (this) {
        is Result.Success -> this
        is Result.Failure -> Result.failure(transform(error.cause ?: RuntimeException(error.message)))
    }
}

private inline fun <T> Result<T>.recoverWith(transform: (DomainError) -> Result<T>): Result<T> = when (this) {
    is Result.Success -> this
    is Result.Failure -> transform(error)
}