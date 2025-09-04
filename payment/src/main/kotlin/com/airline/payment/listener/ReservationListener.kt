package com.airline.payment.listener

import com.airline.payment.annotation.KafkaOtelTrace
import com.airline.payment.service.PaymentService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ReservationListener(
    private val paymentService: PaymentService
) {

    @KafkaListener(topics = ["seat.reserved"], groupId = "payment")
    @KafkaOtelTrace(
        spanName = "process-seat-reserved",
        attributes = ["event.type=seat.reserved", "service=payment"],
        recordMessageContent = true
    )
    fun seatReservedListener(message: String) {
        println("Received seat.reserved event: $message")

        paymentService.pay()

    }
}