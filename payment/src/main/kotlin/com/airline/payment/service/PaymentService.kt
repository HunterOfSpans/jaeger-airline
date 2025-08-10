package com.airline.payment.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class PaymentService (
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    fun pay() {
        kafkaTemplate.send("payment.approved","A payment is approved")
    }

}