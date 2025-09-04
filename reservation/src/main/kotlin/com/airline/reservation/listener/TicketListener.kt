package com.airline.reservation.listener

import com.airline.reservation.annotation.KafkaOtelTrace
import com.airline.reservation.service.ReservationService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
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
    fun ticketIssuedListener(record: ConsumerRecord<String, String>) {
        println(record.value())
        reservationService.confirm()

    }
}