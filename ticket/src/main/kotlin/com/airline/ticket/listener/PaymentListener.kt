package com.airline.ticket.listener

import com.airline.tracing.annotation.KafkaOtelTrace
import com.airline.ticket.service.TicketService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class PaymentListener(
    private val ticketService: TicketService
) {
    @KafkaListener(topics = ["payment.approved"], groupId = "ticket")
    @KafkaOtelTrace(
        spanName = "process-payment-approved", 
        attributes = ["event.type=payment.approved", "service=ticket"]
    )
    fun paymentApprovedListener(
        @Payload message: String,
        @Headers headers: MessageHeaders
    ) {
        println(message)
        ticketService.issue()
    }
}