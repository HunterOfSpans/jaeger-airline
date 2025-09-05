package com.airline.payment.service

import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.dto.PaymentStatus
import com.airline.payment.entity.Payment
import com.airline.payment.mapper.PaymentMapper
import com.airline.payment.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class PaymentService (
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val paymentRepository: PaymentRepository,
    private val paymentMapper: PaymentMapper
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    
    fun processPayment(request: PaymentRequest): PaymentResponse {
        logger.info("Processing payment for reservation: {}, amount: {}", 
                   request.reservationId, request.amount)
        
        val paymentId = "PAY-${UUID.randomUUID().toString().take(8)}"
        
        // 엔티티 생성
        val payment = paymentMapper.toEntity(request, paymentId)
        
        // 간단한 결제 로직 (실제로는 외부 결제 시스템 연동)
        val isSuccessful = simulatePaymentProcessing(request)
        
        // 결제 결과 업데이트
        payment.status = if (isSuccessful) PaymentStatus.SUCCESS else PaymentStatus.FAILED
        payment.message = if (isSuccessful) "Payment completed successfully" else "Payment failed"
        
        // 결제 정보 저장
        val savedPayment = paymentRepository.save(payment)
        
        // Kafka 이벤트 발송 (기존 로직)
        if (isSuccessful) {
            kafkaTemplate.send("payment.approved", "Payment approved for reservation: ${request.reservationId}")
        }
        
        logger.info("Payment processed: {} - {}", paymentId, savedPayment.status)
        return paymentMapper.toResponse(savedPayment)
    }
    
    fun getPaymentById(paymentId: String): PaymentResponse? {
        val payment = paymentRepository.findById(paymentId)
        return payment?.let { paymentMapper.toResponse(it) }
    }
    
    fun cancelPayment(paymentId: String): PaymentResponse? {
        val payment = paymentRepository.findById(paymentId)
        
        if (payment != null && payment.status == PaymentStatus.SUCCESS) {
            payment.status = PaymentStatus.CANCELLED
            payment.message = "Payment cancelled"
            
            val cancelledPayment = paymentRepository.save(payment)
            
            // 취소 이벤트 발송
            kafkaTemplate.send("payment.cancelled", "Payment cancelled: $paymentId")
            logger.info("Payment cancelled: {}", paymentId)
            return paymentMapper.toResponse(cancelledPayment)
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