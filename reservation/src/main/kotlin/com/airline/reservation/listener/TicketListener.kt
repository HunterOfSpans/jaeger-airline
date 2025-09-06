package com.airline.reservation.listener

import com.airline.reservation.annotation.KafkaOtelTrace
import com.airline.reservation.service.ReservationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class TicketListener (
    private val reservationService: ReservationService
) {

    @KafkaListener(topics = ["ticket.issued"], groupId = "reservation")
    @KafkaOtelTrace(
        spanName = "process-ticket-issued",
        attributes = ["event.type=ticket.issued", "service=reservation"]
    )
    fun ticketIssuedListener(
        @Payload message: String,
        @Headers headers: MessageHeaders
    ) {
        println(message)
        reservationService.confirm()
    }
}