package com.airline.payment.listener

import com.airline.payment.service.PaymentService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ReservationListener(
    private val paymentService: PaymentService
) {

    @KafkaListener(topics = ["reservation.created"], groupId = "payment")
    fun reservationCreatedListener(message: String) {
        println(message)

        paymentService.pay()

    }
}