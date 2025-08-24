package com.airline.payment.service

import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.dto.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class PaymentService (
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    private val payments = ConcurrentHashMap<String, PaymentResponse>()
    
    fun processPayment(request: PaymentRequest): PaymentResponse {
        logger.info("Processing payment for reservation: {}, amount: {}", 
                   request.reservationId, request.amount)
        
        val paymentId = "PAY-${UUID.randomUUID().toString().take(8)}"
        
        // 간단한 결제 로직 (실제로는 외부 결제 시스템 연동)
        val isSuccessful = simulatePaymentProcessing(request)
        
        val status = if (isSuccessful) PaymentStatus.SUCCESS else PaymentStatus.FAILED
        val message = if (isSuccessful) "Payment completed successfully" else "Payment failed"
        
        val response = PaymentResponse(
            paymentId = paymentId,
            status = status,
            amount = request.amount,
            reservationId = request.reservationId,
            processedAt = LocalDateTime.now(),
            message = message
        )
        
        // 결제 정보 저장
        payments[paymentId] = response
        
        // Kafka 이벤트 발송 (기존 로직)
        if (isSuccessful) {
            kafkaTemplate.send("payment.approved", "Payment approved for reservation: ${request.reservationId}")
        }
        
        logger.info("Payment processed: {} - {}", paymentId, status)
        return response
    }
    
    fun getPaymentById(paymentId: String): PaymentResponse? {
        return payments[paymentId]
    }
    
    fun cancelPayment(paymentId: String): PaymentResponse? {
        val payment = payments[paymentId]
        if (payment != null && payment.status == PaymentStatus.SUCCESS) {
            val cancelledPayment = payment.copy(
                status = PaymentStatus.CANCELLED,
                message = "Payment cancelled"
            )
            payments[paymentId] = cancelledPayment
            
            // 취소 이벤트 발송
            kafkaTemplate.send("payment.cancelled", "Payment cancelled: $paymentId")
            logger.info("Payment cancelled: {}", paymentId)
            return cancelledPayment
        }
        return null
    }
    
    private fun simulatePaymentProcessing(request: PaymentRequest): Boolean {
        // 90% 성공률로 시뮬레이션
        return when {
            request.amount > BigDecimal("1000000") -> Math.random() > 0.3 // 큰 금액은 70% 성공률
            request.amount > BigDecimal("500000") -> Math.random() > 0.1 // 중간 금액은 90% 성공률  
            else -> Math.random() > 0.05 // 작은 금액은 95% 성공률
        }
    }
    
    // 기존 메서드 호확성 유지
    fun pay() {
        kafkaTemplate.send("payment.approved","A payment is approved")
    }
}