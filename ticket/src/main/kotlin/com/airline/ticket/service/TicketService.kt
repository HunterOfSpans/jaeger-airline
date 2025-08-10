package com.airline.ticket.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class TicketService (
    private val kafkaTemplate: KafkaTemplate<String, String>
){

    fun issue(){
        kafkaTemplate.send("ticket.issued","A ticket issued")
    }
}