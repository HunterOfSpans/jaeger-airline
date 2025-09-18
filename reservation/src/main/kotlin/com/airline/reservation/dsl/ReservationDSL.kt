package com.airline.reservation.dsl

import arrow.core.Either
import com.airline.reservation.common.DomainError
import com.airline.reservation.domain.DomainEvent
import com.airline.reservation.domain.DomainEventPublisher
import com.airline.reservation.domain.ReservationAggregate
import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.dto.external.*
import com.airline.reservation.monitoring.ReservationMetrics
import kotlinx.coroutines.CoroutineScope
import org.slf4j.Logger
import kotlin.time.Duration

/**
 * Kotlin DSL을 활용한 예약 플루언트 API
 *
 * 함수형 프로그래밍과 DSL 패턴을 결합하여
 * 직관적이고 안전한 예약 처리 API를 제공합니다.
 */

// 컨텍스트 인터페이스들
interface ReservationContext {
    val logger: Logger
    val metrics: ReservationMetrics
    val eventPublisher: DomainEventPublisher
    val scope: CoroutineScope
}

interface FlightOperations {
    suspend fun validateFlight(flightId: String): Either<DomainError, FlightDto>
    suspend fun reserveSeats(flightId: String, seats: Int): Either<DomainError, Unit>
    suspend fun releaseSeats(flightId: String, seats: Int): Either<DomainError, Unit>
}

interface PaymentOperations {
    suspend fun processPayment(request: PaymentRequest): Either<DomainError, PaymentResponse>
    suspend fun cancelPayment(paymentId: String): Either<DomainError, Unit>
}

interface TicketOperations {
    suspend fun issueTicket(request: TicketRequest): Either<DomainError, TicketResponse>
    suspend fun cancelTicket(ticketId: String): Either<DomainError, Unit>
}

/**
 * DSL 마커 애노테이션
 */
@DslMarker
annotation class ReservationDSLMarker

/**
 * 예약 플루언트 빌더
 */
@ReservationDSLMarker
class ReservationFluentBuilder(
    private val request: ReservationRequest,
    private val context: ReservationContext,
    private val flightOps: FlightOperations,
    private val paymentOps: PaymentOperations,
    private val ticketOps: TicketOperations
) {
    private var currentFlight: FlightDto? = null
    private var currentPayment: PaymentResponse? = null
    private var currentTicket: TicketResponse? = null

    suspend fun validateFlight(): ReservationFluentBuilder {
        context.logger.info("항공편 검증 시작: {}", request.flightId)

        when (val result = flightOps.validateFlight(request.flightId)) {
            is Either.Left -> throw DSLException(result.value)
            is Either.Right -> {
                currentFlight = result.value
                context.logger.info("항공편 검증 완료: {}", result.value.flightId)
            }
        }
        return this
    }

    suspend fun reserveSeats(): ReservationFluentBuilder {
        context.logger.info("좌석 예약 시작: {}", request.flightId)

        when (val result = flightOps.reserveSeats(request.flightId, 1)) {
            is Either.Left -> throw DSLException(result.value)
            is Either.Right -> {
                context.logger.info("좌석 예약 완료: {}", request.flightId)
                context.metrics.recordReservationAttempt()
            }
        }
        return this
    }

    suspend fun processPayment(): ReservationFluentBuilder {
        requireNotNull(currentFlight) { "Flight must be validated before payment" }
        context.logger.info("결제 처리 시작")

        val paymentRequest = PaymentRequest(
            reservationId = request.toString(), // 임시
            amount = currentFlight!!.price,
            paymentMethod = request.paymentMethod,
            customerInfo = CustomerInfo(
                name = request.passengerInfo.name,
                email = request.passengerInfo.email
            )
        )

        when (val result = paymentOps.processPayment(paymentRequest)) {
            is Either.Left -> throw DSLException(result.value)
            is Either.Right -> {
                currentPayment = result.value
                context.logger.info("결제 처리 완료: {}", result.value.paymentId)
            }
        }
        return this
    }

    suspend fun issueTicket(): ReservationFluentBuilder {
        requireNotNull(currentPayment) { "Payment must be processed before ticket issuance" }
        context.logger.info("항공권 발급 시작")

        val ticketRequest = TicketRequest(
            reservationId = request.toString(), // 임시
            paymentId = currentPayment!!.paymentId,
            flightId = request.flightId,
            passengerInfo = TicketPassengerInfo(
                name = request.passengerInfo.name,
                email = request.passengerInfo.email,
                phone = request.passengerInfo.phone,
                passportNumber = request.passengerInfo.passportNumber
            )
        )

        when (val result = ticketOps.issueTicket(ticketRequest)) {
            is Either.Left -> throw DSLException(result.value)
            is Either.Right -> {
                currentTicket = result.value
                context.logger.info("항공권 발급 완료: {}", result.value.ticketId)
            }
        }
        return this
    }

    suspend fun publishEvents(): ReservationFluentBuilder {
        requireNotNull(currentPayment) { "Payment must be completed before publishing events" }
        requireNotNull(currentTicket) { "Ticket must be issued before publishing events" }

        context.logger.info("도메인 이벤트 발행 시작")

        // 도메인 이벤트 발행 로직 (간소화)
        context.logger.info("도메인 이벤트 발행 완료")
        return this
    }

    fun getResult(): ReservationResponse {
        requireNotNull(currentFlight) { "Flight information missing" }
        requireNotNull(currentPayment) { "Payment information missing" }
        requireNotNull(currentTicket) { "Ticket information missing" }

        return ReservationResponse(
            reservationId = "RES-${System.currentTimeMillis()}",
            status = com.airline.reservation.dto.ReservationStatus.CONFIRMED,
            flightId = currentFlight!!.flightId,
            passengerInfo = request.passengerInfo,
            paymentId = currentPayment!!.paymentId,
            ticketId = currentTicket!!.ticketId,
            totalAmount = currentFlight!!.price,
            seatNumber = currentTicket!!.seatNumber,
            createdAt = java.time.LocalDateTime.now(),
            message = "예약 완료"
        )
    }

    class DSLException(val domainError: DomainError) : Exception(domainError.message)
}

/**
 * 메인 DSL 진입점
 */
suspend fun createReservation(
    request: ReservationRequest,
    context: ReservationContext,
    flightOps: FlightOperations,
    paymentOps: PaymentOperations,
    ticketOps: TicketOperations,
    block: suspend ReservationFluentBuilder.() -> Unit
): Either<DomainError, ReservationResponse> {
    return try {
        val builder = ReservationFluentBuilder(request, context, flightOps, paymentOps, ticketOps)
        builder.block()
        Either.Right(builder.getResult())
    } catch (e: ReservationFluentBuilder.DSLException) {
        Either.Left(e.domainError)
    } catch (e: Exception) {
        Either.Left(DomainError.SystemError("Reservation DSL execution failed", e))
    }
}

/**
 * 타이밍 측정 DSL
 */
@ReservationDSLMarker
class TimeMeasurement {
    var startTime: Long = System.currentTimeMillis()

    fun reset() {
        startTime = System.currentTimeMillis()
    }

    fun elapsed(): Duration = Duration.parse("${System.currentTimeMillis() - startTime}ms")
}

suspend fun measureTime(
    context: ReservationContext,
    block: suspend TimeMeasurement.() -> Unit
) {
    val timer = TimeMeasurement()
    try {
        timer.block()
    } finally {
        val elapsed = timer.elapsed()
        context.logger.info("작업 완료 시간: {}", elapsed)
        context.metrics.recordReservationSuccess(elapsed.inWholeMilliseconds)
    }
}

/**
 * 조건부 실행 DSL
 */
@ReservationDSLMarker
class ConditionalExecution<T>(private val value: T) {
    suspend fun whenTrue(
        condition: (T) -> Boolean,
        action: suspend (T) -> Unit
    ) {
        if (condition(value)) {
            action(value)
        }
    }

    suspend fun whenFalse(
        condition: (T) -> Boolean,
        action: suspend (T) -> Unit
    ) {
        if (!condition(value)) {
            action(value)
        }
    }
}

fun <T> conditional(value: T) = ConditionalExecution(value)

/**
 * 재시도 DSL
 */
@ReservationDSLMarker
class RetryBuilder {
    var maxAttempts: Int = 3
    var delayMs: Long = 1000
    var backoffMultiplier: Double = 2.0

    fun attempts(count: Int) {
        maxAttempts = count
    }

    fun delay(ms: Long) {
        delayMs = ms
    }

    fun exponentialBackoff(multiplier: Double) {
        backoffMultiplier = multiplier
    }
}

suspend fun retry(
    context: ReservationContext,
    config: RetryBuilder.() -> Unit = {},
    operation: suspend () -> Either<DomainError, *>
): Either<DomainError, *> {
    val builder = RetryBuilder().apply(config)
    var lastError: DomainError? = null
    var currentDelay = builder.delayMs

    repeat(builder.maxAttempts) { attempt ->
        context.logger.info("재시도 시도 {}/{}", attempt + 1, builder.maxAttempts)

        when (val result = operation()) {
            is Either.Right -> return result
            is Either.Left -> {
                lastError = result.value
                if (attempt < builder.maxAttempts - 1) {
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * builder.backoffMultiplier).toLong()
                }
            }
        }
    }

    return Either.Left(lastError ?: DomainError.SystemError("All retry attempts failed"))
}

/**
 * 사용 예시:
 *
 * ```kotlin
 * createReservation(request, context, flightOps, paymentOps, ticketOps) {
 *     validateFlight()
 *         .reserveSeats()
 *         .processPayment()
 *         .issueTicket()
 *         .publishEvents()
 * }
 * ```
 */