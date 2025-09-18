package com.airline.reservation.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/**
 * 도메인 이벤트 정의
 *
 * Event Sourcing 패턴을 활용하여 모든 비즈니스 이벤트를 추적합니다.
 * sealed class로 타입 안전성을 보장하고, 각 이벤트는 불변입니다.
 */
sealed class DomainEvent(
    open val aggregateId: String,
    open val eventId: String = UUID.randomUUID().toString(),
    open val timestamp: Instant = Instant.now(),
    open val version: Long = 1L
) {
    data class ReservationInitiated(
        override val aggregateId: String,
        val flightId: String,
        val passengerName: String,
        val passengerEmail: String,
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    data class SeatsReserved(
        override val aggregateId: String,
        val flightId: String,
        val seatsCount: Int,
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    data class PaymentProcessed(
        override val aggregateId: String,
        val paymentId: String,
        val amount: java.math.BigDecimal,
        val paymentMethod: String,
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    data class TicketIssued(
        override val aggregateId: String,
        val ticketId: String,
        val seatNumber: String,
        val flightId: String,
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    data class ReservationCompleted(
        override val aggregateId: String,
        val totalAmount: java.math.BigDecimal,
        val completionDuration: Long, // milliseconds
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    data class ReservationFailed(
        override val aggregateId: String,
        val reason: String,
        val failureStage: String, // FLIGHT_VALIDATION, PAYMENT, TICKET_ISSUANCE
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    data class ReservationCancelled(
        override val aggregateId: String,
        val cancellationReason: String,
        val refundAmount: java.math.BigDecimal?,
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    data class CompensationExecuted(
        override val aggregateId: String,
        val compensationType: String, // SEAT_RELEASE, PAYMENT_REFUND, TICKET_CANCELLATION
        val success: Boolean,
        val details: String,
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent(aggregateId, eventId, timestamp)

    /**
     * 이벤트를 JSON으로 직렬화
     */
    fun toJson(): String = when (this) {
        is ReservationInitiated -> """
            {
                "type": "ReservationInitiated",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "flightId": "$flightId",
                "passengerName": "$passengerName",
                "passengerEmail": "$passengerEmail"
            }
        """.trimIndent()
        is SeatsReserved -> """
            {
                "type": "SeatsReserved",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "flightId": "$flightId",
                "seatsCount": $seatsCount
            }
        """.trimIndent()
        is PaymentProcessed -> """
            {
                "type": "PaymentProcessed",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "paymentId": "$paymentId",
                "amount": $amount,
                "paymentMethod": "$paymentMethod"
            }
        """.trimIndent()
        is TicketIssued -> """
            {
                "type": "TicketIssued",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "ticketId": "$ticketId",
                "seatNumber": "$seatNumber",
                "flightId": "$flightId"
            }
        """.trimIndent()
        is ReservationCompleted -> """
            {
                "type": "ReservationCompleted",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "totalAmount": $totalAmount,
                "completionDuration": $completionDuration
            }
        """.trimIndent()
        is ReservationFailed -> """
            {
                "type": "ReservationFailed",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "reason": "$reason",
                "failureStage": "$failureStage"
            }
        """.trimIndent()
        is ReservationCancelled -> """
            {
                "type": "ReservationCancelled",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "cancellationReason": "$cancellationReason",
                "refundAmount": $refundAmount
            }
        """.trimIndent()
        is CompensationExecuted -> """
            {
                "type": "CompensationExecuted",
                "aggregateId": "$aggregateId",
                "eventId": "$eventId",
                "timestamp": "$timestamp",
                "compensationType": "$compensationType",
                "success": $success,
                "details": "$details"
            }
        """.trimIndent()
    }

    /**
     * 이벤트 타입별 토픽 매핑
     */
    fun getKafkaTopic(): String = when (this) {
        is ReservationInitiated -> "domain.reservation.initiated"
        is SeatsReserved -> "domain.seats.reserved"
        is PaymentProcessed -> "domain.payment.processed"
        is TicketIssued -> "domain.ticket.issued"
        is ReservationCompleted -> "domain.reservation.completed"
        is ReservationFailed -> "domain.reservation.failed"
        is ReservationCancelled -> "domain.reservation.cancelled"
        is CompensationExecuted -> "domain.compensation.executed"
    }
}

/**
 * 도메인 이벤트 저장소
 *
 * Event Sourcing을 위한 이벤트 스트림 관리
 */
interface EventStore {
    suspend fun saveEvent(event: DomainEvent)
    suspend fun getEvents(aggregateId: String): Flow<DomainEvent>
    suspend fun getEventsAfter(aggregateId: String, version: Long): Flow<DomainEvent>
}

/**
 * 인메모리 이벤트 저장소 (데모용)
 */
@Component
class InMemoryEventStore : EventStore {
    private val events = mutableMapOf<String, MutableList<DomainEvent>>()
    private val eventStream = MutableSharedFlow<DomainEvent>(replay = 100)

    override suspend fun saveEvent(event: DomainEvent) {
        events.computeIfAbsent(event.aggregateId) { mutableListOf() }.add(event)
        eventStream.emit(event)
    }

    override suspend fun getEvents(aggregateId: String): Flow<DomainEvent> {
        return kotlinx.coroutines.flow.flowOf(*events[aggregateId]?.toTypedArray() ?: emptyArray())
    }

    override suspend fun getEventsAfter(aggregateId: String, version: Long): Flow<DomainEvent> {
        val aggregateEvents = events[aggregateId] ?: emptyList()
        return kotlinx.coroutines.flow.flowOf(*aggregateEvents.filter { it.version > version }.toTypedArray())
    }

    fun getAllEvents(): Flow<DomainEvent> = eventStream.asSharedFlow()
}

/**
 * 도메인 이벤트 발행자
 *
 * 이벤트를 Kafka와 내부 이벤트 스트림에 동시 발행
 */
@Component
class DomainEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val eventStore: EventStore = InMemoryEventStore()
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(DomainEventPublisher::class.java)

    suspend fun publish(event: DomainEvent) {
        try {
            // 1. 이벤트 저장소에 저장
            eventStore.saveEvent(event)

            // 2. Kafka에 발행
            kafkaTemplate.send(event.getKafkaTopic(), event.aggregateId, event.toJson())

            logger.info("도메인 이벤트 발행: {} - {}", event::class.simpleName, event.aggregateId)
        } catch (e: Exception) {
            logger.error("도메인 이벤트 발행 실패: {}", event, e)
            throw e
        }
    }

    // 편의 메서드들
    suspend fun publishReservationInitiated(aggregateId: String, flightId: String, passengerName: String, passengerEmail: String) {
        publish(DomainEvent.ReservationInitiated(aggregateId, flightId, passengerName, passengerEmail))
    }

    suspend fun publishSeatsReserved(aggregateId: String, flightId: String, seatsCount: Int) {
        publish(DomainEvent.SeatsReserved(aggregateId, flightId, seatsCount))
    }

    suspend fun publishPaymentProcessed(aggregateId: String, paymentId: String, amount: java.math.BigDecimal, paymentMethod: String) {
        publish(DomainEvent.PaymentProcessed(aggregateId, paymentId, amount, paymentMethod))
    }

    suspend fun publishTicketIssued(aggregateId: String, ticketId: String, seatNumber: String, flightId: String) {
        publish(DomainEvent.TicketIssued(aggregateId, ticketId, seatNumber, flightId))
    }

    suspend fun publishReservationCompleted(reservation: ReservationAggregate) {
        publish(DomainEvent.ReservationCompleted(
            aggregateId = reservation.reservationId.value,
            totalAmount = reservation.totalAmount,
            completionDuration = System.currentTimeMillis() - reservation.createdAt.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        ))
    }

    suspend fun publishReservationFailed(aggregateId: String, reason: String, stage: String) {
        publish(DomainEvent.ReservationFailed(aggregateId, reason, stage))
    }

    suspend fun publishReservationCancelled(aggregateId: String) {
        publish(DomainEvent.ReservationCancelled(aggregateId, "사용자 요청", null))
    }

    suspend fun publishCompensationExecuted(aggregateId: String, type: String, success: Boolean, details: String) {
        publish(DomainEvent.CompensationExecuted(aggregateId, type, success, details))
    }
}