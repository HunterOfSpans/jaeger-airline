package com.airline.ticket.listener

import com.airline.tracing.annotation.KafkaOtelTrace
import com.airline.ticket.service.TicketService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 결제 승인 이벤트를 수신하여 항공권을 발급하는 리스너
 *
 * 이벤트 체인: reservation.requested → seat.reserved → payment.approved → ticket.issued
 */
@Component
class PaymentListener(
    private val ticketService: TicketService,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(PaymentListener::class.java)
    private val objectMapper = ObjectMapper()

    @KafkaListener(topics = ["payment.approved"], groupId = "ticket")
    @KafkaOtelTrace(
        spanName = "process-payment-approved",
        attributes = ["event.type=payment.approved", "service=ticket"],
        recordMessageContent = true
    )
    fun paymentApprovedListener(
        @Payload message: String,
        @Headers headers: MessageHeaders
    ) {
        logger.info("Received payment.approved event: {}", message)

        try {
            val eventData = parseEventData(message)
            val reservationId = eventData.get("reservationId")?.asText() ?: throw IllegalArgumentException("Missing reservationId")
            val flightId = eventData.get("flightId")?.asText() ?: "UNKNOWN"
            val paymentId = eventData.get("paymentId")?.asText() ?: "UNKNOWN"
            val seats = eventData.get("seats")?.asInt() ?: 1

            // 항공권 발급 처리
            val ticketId = issueTicket(reservationId, flightId, paymentId, seats)

            // 항공권 발급 이벤트 발행
            publishTicketIssuedEvent(reservationId, flightId, paymentId, ticketId, seats)

            logger.info("Ticket issued successfully for reservation: {}", reservationId)
        } catch (e: Exception) {
            logger.error("Failed to process payment.approved event: {}", e.message, e)
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
     * 항공권을 발급하고 항공권 ID를 반환합니다.
     */
    private fun issueTicket(reservationId: String, flightId: String, paymentId: String, seats: Int): String {
        val ticketId = "TKT-${UUID.randomUUID().toString().take(8)}"
        val seatNumber = generateSeatNumber()

        logger.info("Issuing ticket {} for reservation {} - Seat: {}",
            ticketId, reservationId, seatNumber)

        // 항공권 발급 시간 시뮬레이션
        Thread.sleep(80)

        return ticketId
    }

    /**
     * 좌석 번호를 생성합니다.
     */
    private fun generateSeatNumber(): String {
        val row = (1..30).random()
        val column = listOf("A", "B", "C", "D", "E", "F").random()
        return "$row$column"
    }

    /**
     * 항공권 발급 이벤트를 발행합니다.
     */
    private fun publishTicketIssuedEvent(
        reservationId: String,
        flightId: String,
        paymentId: String,
        ticketId: String,
        seats: Int
    ) {
        val seatNumber = generateSeatNumber()
        val eventData = mapOf(
            "reservationId" to reservationId,
            "flightId" to flightId,
            "paymentId" to paymentId,
            "ticketId" to ticketId,
            "seatNumber" to seatNumber,
            "seats" to seats,
            "ticketStatus" to "ISSUED",
            "timestamp" to System.currentTimeMillis()
        )

        val eventJson = objectMapper.writeValueAsString(eventData)
        kafkaTemplate.send("ticket.issued", reservationId, eventJson)
        logger.info("Published ticket.issued event: {}", eventJson)
    }
}