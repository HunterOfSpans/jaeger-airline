package com.airline.ticket.listener

import com.airline.ticket.annotation.KafkaOtelTrace
import com.airline.ticket.service.TicketService
import org.springframework.kafka.annotation.KafkaListener
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
    fun paymentApprovedListener(message: String) {
        println(message)

        ticketService.issue()
    }

}