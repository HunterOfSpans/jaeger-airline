package com.airline.payment.service

import com.airline.payment.config.PaymentConfig
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

/**
 * 결제 관리 서비스
 * 
 * 항공권 예약에 대한 결제 처리, 취소, 상태 조회 등의 비즈니스 로직을 담당합니다.
 * Kafka를 통한 이벤트 발행, 다양한 결제 방식 지원, 실패 시 보상 트랜잭션을 포함합니다.
 * 결제 성공률은 결제 금액에 따라 차등 적용되며, 실제 결제 시스템과의 연동을 시뮬레이션합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Service
class PaymentService (
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val paymentRepository: PaymentRepository,
    private val paymentMapper: PaymentMapper,
    private val paymentConfig: PaymentConfig
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    
    /**
     * 예약에 대한 결제를 처리합니다.
     * 
     * 결제 요청을 받아 결제 시스템과 연동하여 처리하고, 성공 시 승인 이벤트를 발행합니다.
     * 결제 성공률은 금액에 따라 다르게 적용되며, 큰 금액일수록 실패 확률이 높아집니다.
     * 
     * @param request 결제 요청 정보 (예약 ID, 결제 금액, 결제 방법, 고객 정보 포함)
     * @return 결제 처리 결과 (결제 ID, 상태, 처리 시간 등)
     */
    fun processPayment(request: PaymentRequest): PaymentResponse {
        logger.info("예약 {}에 대한 {}원 결제 처리 시작", request.reservationId, request.amount)
        
        val paymentId = generatePaymentId()
        val payment = createPaymentEntity(request, paymentId)
        
        // 결제 시스템 연동 시뮬레이션
        val isSuccessful = processExternalPayment(request)
        
        // 결제 결과 반영
        updatePaymentResult(payment, isSuccessful)
        val savedPayment = paymentRepository.save(payment)
        
        // 성공 시 이벤트 발행
        if (isSuccessful) {
            publishPaymentApprovedEvent(request.reservationId)
        }
        
        logger.info("결제 처리 완료: {} - {}", paymentId, savedPayment.status)
        return paymentMapper.toResponse(savedPayment)
    }
    
    /**
     * 결제 ID로 결제 정보를 조회합니다.
     * 
     * @param paymentId 결제 식별자
     * @return 결제 정보 DTO, 존재하지 않으면 null
     */
    fun getPaymentById(paymentId: String): PaymentResponse? {
        logger.info("결제 정보 조회: {}", paymentId)
        val payment = paymentRepository.findById(paymentId)
        return payment?.let { paymentMapper.toResponse(it) }
    }
    
    /**
     * 성공한 결제를 취소합니다.
     * 
     * 보상 트랜잭션의 일환으로 호출되며, 성공 상태의 결제만 취소 가능합니다.
     * 취소 성공 시 취소 이벤트를 발행합니다.
     * 
     * @param paymentId 취소할 결제 식별자
     * @return 취소된 결제 정보, 취소 불가능하면 null
     */
    fun cancelPayment(paymentId: String): PaymentResponse? {
        logger.info("결제 취소 요청: {}", paymentId)
        val payment = paymentRepository.findById(paymentId)
        
        if (canCancelPayment(payment)) {
            return processCancellation(payment!!, paymentId)
        }
        
        logger.warn("취소할 수 없는 결제: {} (상태: {})", paymentId, payment?.status)
        return null
    }
    
    /**
     * 외부 결제 시스템과의 연동을 시뮬레이션합니다.
     * 
     * 결제 금액에 따라 차등화된 성공률을 적용:
     * - 100만원 초과: 70% 성공률
     * - 50만원 초과: 90% 성공률  
     * - 50만원 이하: 95% 성공률
     */
    private fun processExternalPayment(request: PaymentRequest): Boolean {
        val thresholds = paymentConfig.amountThresholds
        val successRates = paymentConfig.successRates
        
        return when {
            request.amount > thresholds.high -> Math.random() < successRates.highAmount
            request.amount > thresholds.medium -> Math.random() < successRates.mediumAmount
            else -> Math.random() < successRates.lowAmount
        }
    }
    
    /**
     * 결제 ID를 생성합니다.
     */
    private fun generatePaymentId(): String {
        val idConfig = paymentConfig.idGeneration
        return "${idConfig.prefix}${UUID.randomUUID().toString().take(idConfig.uuidLength)}"
    }
    
    /**
     * 결제 요청으로부터 결제 엔티티를 생성합니다.
     */
    private fun createPaymentEntity(request: PaymentRequest, paymentId: String) =
        paymentMapper.toEntity(request, paymentId)
    
    /**
     * 결제 결과를 엔티티에 반영합니다.
     */
    private fun updatePaymentResult(payment: Payment, isSuccessful: Boolean) {
        payment.status = if (isSuccessful) PaymentStatus.SUCCESS else PaymentStatus.FAILED
        payment.message = if (isSuccessful) "결제 성공" else "결제 실패"
    }
    
    /**
     * 결제 승인 이벤트를 발행합니다.
     */
    private fun publishPaymentApprovedEvent(reservationId: String) {
        kafkaTemplate.send("payment.approved", "Payment approved for reservation: $reservationId")
    }
    
    /**
     * 결제 취소 가능 여부를 확인합니다.
     */
    private fun canCancelPayment(payment: Payment?): Boolean {
        return payment != null && payment.status == PaymentStatus.SUCCESS
    }
    
    /**
     * 결제 취소 처리를 수행합니다.
     */
    private fun processCancellation(payment: Payment, paymentId: String): PaymentResponse {
        payment.status = PaymentStatus.CANCELLED
        payment.message = "결제 취소됨"
        
        val cancelledPayment = paymentRepository.save(payment)
        
        // 취소 이벤트 발행
        kafkaTemplate.send("payment.cancelled", "Payment cancelled: $paymentId")
        logger.info("결제 취소 완료: {}", paymentId)
        
        return paymentMapper.toResponse(cancelledPayment)
    }
    
    // 기존 메서드 호환성 유지
    fun pay() {
        kafkaTemplate.send("payment.approved","A payment is approved")
    }
}