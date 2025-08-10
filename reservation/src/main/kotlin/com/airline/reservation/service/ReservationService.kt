package com.airline.reservation.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ReservationService(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    fun reserve() {
        kafkaTemplate.send("reservation.created", "A reservation is created")
    }

    fun confirm() {
        println("A reservation is confirmed")
    }

}