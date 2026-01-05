package com.airline.reservation.listener

import com.airline.tracing.annotation.KafkaOtelTrace
import com.airline.reservation.service.ReservationService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * 항공권 발급 이벤트를 수신하여 예약을 완료하는 리스너
 *
 * 이벤트 체인: reservation.requested → seat.reserved → payment.approved → ticket.issued → reservation.completed
 */
@Component
class TicketListener(
    private val reservationService: ReservationService,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(TicketListener::class.java)
    private val objectMapper = ObjectMapper()

    @KafkaListener(topics = ["ticket.issued"], groupId = "reservation")
    @KafkaOtelTrace(
        spanName = "process-ticket-issued",
        attributes = ["event.type=ticket.issued", "service=reservation"],
        recordMessageContent = true
    )
    fun ticketIssuedListener(
        @Payload message: String,
        @Headers headers: MessageHeaders
    ) {
        logger.info("Received ticket.issued event: {}", message)

        try {
            val eventData = parseEventData(message)
            val reservationId = eventData.get("reservationId")?.asText() ?: throw IllegalArgumentException("Missing reservationId")
            val flightId = eventData.get("flightId")?.asText() ?: "UNKNOWN"
            val paymentId = eventData.get("paymentId")?.asText() ?: "UNKNOWN"
            val ticketId = eventData.get("ticketId")?.asText() ?: "UNKNOWN"
            val seatNumber = eventData.get("seatNumber")?.asText() ?: "UNKNOWN"

            // 예약 확정 처리
            confirmReservation(reservationId, flightId, paymentId, ticketId, seatNumber)

            // 예약 완료 이벤트 발행 (최종 이벤트)
            publishReservationCompletedEvent(reservationId, flightId, paymentId, ticketId, seatNumber)

            logger.info("Reservation completed successfully: {}", reservationId)
        } catch (e: Exception) {
            logger.error("Failed to process ticket.issued event: {}", e.message, e)
            throw e
        }
    }

    /**
     * 이벤트 메시지를 JSON으로 파싱합니다.
     */
    private fun parseEventData(message: String): JsonNode {
        return objectMapper.readTree(message)
    }

    /**
     * 예약을 확정합니다.
     */
    private fun confirmReservation(
        reservationId: String,
        flightId: String,
        paymentId: String,
        ticketId: String,
        seatNumber: String
    ) {
        logger.info("Confirming reservation {} - Flight: {}, Payment: {}, Ticket: {}, Seat: {}",
            reservationId, flightId, paymentId, ticketId, seatNumber)

        // 예약 확정 처리 시간 시뮬레이션
        Thread.sleep(50)

        // 기존 서비스 메서드 호출 (호환성 유지)
        reservationService.confirm()
    }

    /**
     * 예약 완료 이벤트를 발행합니다 (최종 이벤트).
     */
    private fun publishReservationCompletedEvent(
        reservationId: String,
        flightId: String,
        paymentId: String,
        ticketId: String,
        seatNumber: String
    ) {
        val eventData = mapOf(
            "reservationId" to reservationId,
            "flightId" to flightId,
            "paymentId" to paymentId,
            "ticketId" to ticketId,
            "seatNumber" to seatNumber,
            "reservationStatus" to "COMPLETED",
            "timestamp" to System.currentTimeMillis(),
            "message" to "Reservation process completed successfully"
        )

        val eventJson = objectMapper.writeValueAsString(eventData)
        kafkaTemplate.send("reservation.completed", reservationId, eventJson)
        logger.info("Published reservation.completed event: {}", eventJson)
    }
}