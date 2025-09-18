package com.airline.payment.service

import com.airline.payment.config.PaymentConfig
import com.airline.payment.domain.model.PaymentAggregate
import com.airline.payment.domain.repository.PaymentDomainRepository
import com.airline.payment.domain.service.PaymentDomainService
import com.airline.payment.domain.valueobject.PaymentId
import com.airline.payment.domain.exception.PaymentAlreadyProcessedException
import com.airline.payment.domain.exception.InvalidPaymentOperationException
import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.dto.PaymentStatus
import com.airline.payment.entity.Payment
import com.airline.payment.exception.PaymentNotFoundException
import com.airline.payment.exception.PaymentProcessingException
import com.airline.payment.exception.InvalidPaymentRequestException
import com.airline.payment.exception.PaymentAlreadyCancelledException
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
    private val paymentDomainRepository: PaymentDomainRepository,
    private val paymentDomainService: PaymentDomainService,
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
        
        // 입력값 검증
        validatePaymentRequest(request)
        
        val paymentId = generatePaymentId()
        
        // 도메인 애그리게이트 생성
        val paymentAggregate = PaymentAggregate.create(
            paymentId = paymentId,
            reservationId = request.reservationId,
            amount = request.amount,
            paymentMethod = request.paymentMethod,
            customerName = request.customerInfo?.name ?: "",
            customerEmail = request.customerInfo?.email ?: ""
        )
        
        // 결제 가능 여부 검증
        if (!paymentDomainService.canProcessPayment(paymentAggregate)) {
            throw PaymentProcessingException("Payment cannot be processed")
        }
        
        // 외부 결제 시스템 연동
        val processingResult = paymentDomainService.processExternalPayment(paymentAggregate)
        
        if (processingResult.isSuccess()) {
            try {
                paymentAggregate.approve()
                val savedPayment = paymentDomainRepository.save(paymentAggregate)
                
                // 성공 이벤트 발행
                publishPaymentApprovedEvent(request.reservationId)
                
                logger.info("결제 처리 완료: {} - {}", paymentId, savedPayment.getStatus())
                return paymentMapper.toResponse(savedPayment)
                
            } catch (e: PaymentAlreadyProcessedException) {
                logger.error("Payment already processed: {}", e.message)
                throw PaymentProcessingException("Payment already processed")
            }
        } else {
            paymentAggregate.reject(processingResult.message)
            paymentDomainRepository.save(paymentAggregate)
            throw PaymentProcessingException("External payment system declined: ${processingResult.message}")
        }
    }
    
    /**
     * 결제 ID로 결제 정보를 조회합니다.
     * 
     * @param paymentId 결제 식별자
     * @return 결제 정보 DTO, 존재하지 않으면 null
     */
    fun getPaymentById(paymentId: String): PaymentResponse {
        logger.info("결제 정보 조회: {}", paymentId)
        
        if (paymentId.isBlank()) {
            throw InvalidPaymentRequestException("Payment ID cannot be blank")
        }
        
        val domainPaymentId = PaymentId.of(paymentId)
        val payment = paymentDomainRepository.findById(domainPaymentId)
            ?: throw PaymentNotFoundException(paymentId)
            
        return paymentMapper.toResponse(payment)
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
    fun cancelPayment(paymentId: String): PaymentResponse {
        logger.info("결제 취소 요청: {}", paymentId)
        
        if (paymentId.isBlank()) {
            throw InvalidPaymentRequestException("Payment ID cannot be blank")
        }
        
        val domainPaymentId = PaymentId.of(paymentId)
        val payment = paymentDomainRepository.findById(domainPaymentId)
            ?: throw PaymentNotFoundException(paymentId)
        
        if (!payment.canBeCancelled()) {
            if (payment.getStatus() == com.airline.payment.domain.valueobject.PaymentStatus.CANCELLED) {
                throw PaymentAlreadyCancelledException(paymentId)
            } else {
                throw InvalidPaymentRequestException("Payment $paymentId cannot be cancelled (status: ${payment.getStatus()})")
            }
        }
        
        return try {
            payment.cancel()
            val cancelledPayment = paymentDomainRepository.save(payment)
            
            // 취소 이벤트 발행
            kafkaTemplate.send("payment.cancelled", "Payment cancelled: $paymentId")
            logger.info("결제 취소 완료: {}", paymentId)
            
            paymentMapper.toResponse(cancelledPayment)
        } catch (e: InvalidPaymentOperationException) {
            logger.error("Invalid payment operation: {}", e.message)
            throw InvalidPaymentRequestException(e.message ?: "Invalid payment operation")
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
     * 결제 요청 유효성 검증
     */
    private fun validatePaymentRequest(request: PaymentRequest) {
        if (request.reservationId.isBlank()) {
            throw InvalidPaymentRequestException("Reservation ID cannot be blank")
        }
        
        if (request.amount <= BigDecimal.ZERO) {
            throw InvalidPaymentRequestException("Payment amount must be greater than 0")
        }
        
        if (request.paymentMethod.isBlank()) {
            throw InvalidPaymentRequestException("Payment method cannot be blank")
        }
        
        request.customerInfo?.let { customerInfo ->
            if (customerInfo.name.isNullOrBlank()) {
                throw InvalidPaymentRequestException("Customer name cannot be blank")
            }
            if (customerInfo.email.isNullOrBlank()) {
                throw InvalidPaymentRequestException("Customer email cannot be blank")
            }
        } ?: throw InvalidPaymentRequestException("Customer information is required")
    }
    


    /**
     * 결제 승인 이벤트를 발행합니다.
     */
    private fun publishPaymentApprovedEvent(reservationId: String) {
        kafkaTemplate.send("payment.approved", "Payment approved for reservation: $reservationId")
    }
    


    // 기존 메서드 호환성 유지
    fun pay() {
        kafkaTemplate.send("payment.approved","A payment is approved")
    }
}