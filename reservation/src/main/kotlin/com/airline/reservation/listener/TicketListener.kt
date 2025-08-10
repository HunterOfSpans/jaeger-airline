package com.airline.reservation.listener

import com.airline.reservation.service.ReservationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class TicketListener (
    private val reservationService: ReservationService
){


    @KafkaListener(topics = ["ticket.issued"], groupId = "reservation")
    fun ticketIssuedListener(message: String){
        println(message)
        reservationService.confirm()

    }
}