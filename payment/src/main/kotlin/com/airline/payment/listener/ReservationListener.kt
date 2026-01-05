package com.airline.payment.listener

import com.airline.tracing.annotation.KafkaOtelTrace
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

/**
 * 좌석 예약 완료 이벤트를 수신하여 결제를 처리하는 리스너
 *
 * 이벤트 체인: reservation.requested → seat.reserved → payment.approved → ticket.issued
 */
@Component
class ReservationListener(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(ReservationListener::class.java)
    private val objectMapper = ObjectMapper()

    @KafkaListener(topics = ["seat.reserved"], groupId = "payment")
    @KafkaOtelTrace(
        spanName = "process-seat-reserved",
        attributes = ["event.type=seat.reserved", "service=payment"],
        recordMessageContent = true
    )
    fun seatReservedListener(
        @Payload message: String,
        @Headers headers: MessageHeaders
    ) {
        logger.info("Received seat.reserved event: {}", message)

        try {
            val eventData = parseEventData(message)
            val reservationId = eventData.get("reservationId")?.asText() ?: throw IllegalArgumentException("Missing reservationId")
            val flightId = eventData.get("flightId")?.asText() ?: "UNKNOWN"
            val reservedSeats = eventData.get("reservedSeats")?.asInt() ?: 1

            // 결제 처리 (시뮬레이션)
            val paymentId = processPayment(reservationId, flightId, reservedSeats)

            // 결제 승인 이벤트 발행
            publishPaymentApprovedEvent(reservationId, flightId, paymentId, reservedSeats)

            logger.info("Payment processed successfully for reservation: {}", reservationId)
        } catch (e: Exception) {
            logger.error("Failed to process seat.reserved event: {}", e.message, e)
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
     * 결제를 처리하고 결제 ID를 반환합니다.
     */
    private fun processPayment(reservationId: String, flightId: String, seats: Int): String {
        val paymentId = "PAY-${UUID.randomUUID().toString().take(8)}"
        val amount = BigDecimal(seats * 150000) // 좌석당 15만원

        logger.info("Processing payment {} for reservation {} - Amount: {}원",
            paymentId, reservationId, amount)

        // 실제로는 PaymentService.processPayment()를 호출하겠지만,
        // 여기서는 이벤트 체인 데모를 위해 간단히 시뮬레이션
        Thread.sleep(100) // 결제 처리 시간 시뮬레이션

        return paymentId
    }

    /**
     * 결제 승인 이벤트를 발행합니다.
     */
    private fun publishPaymentApprovedEvent(
        reservationId: String,
        flightId: String,
        paymentId: String,
        seats: Int
    ) {
        val eventData = mapOf(
            "reservationId" to reservationId,
            "flightId" to flightId,
            "paymentId" to paymentId,
            "seats" to seats,
            "amount" to (seats * 150000),
            "paymentStatus" to "APPROVED",
            "timestamp" to System.currentTimeMillis()
        )

        val eventJson = objectMapper.writeValueAsString(eventData)
        kafkaTemplate.send("payment.approved", reservationId, eventJson)
        logger.info("Published payment.approved event: {}", eventJson)
    }
}