package com.airline.payment.domain.model

import com.airline.payment.domain.event.PaymentDomainEvent
import com.airline.payment.domain.exception.InvalidPaymentOperationException
import com.airline.payment.domain.exception.PaymentAlreadyProcessedException
import com.airline.payment.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Payment Aggregate Root
 * 
 * 결제 도메인의 애그리게이트 루트
 * 결제 처리, 취소, 상태 변경 등의 비즈니스 로직을 캡슐화
 */
class PaymentAggregate private constructor() {
    
    private var paymentId: PaymentId? = null
    private var reservationId: ReservationId? = null
    private var amount: PaymentAmount? = null
    private var paymentMethod: PaymentMethod? = null
    private var customerInfo: CustomerInfo? = null
    private var status: PaymentStatus = PaymentStatus.PENDING
    private var processedAt: LocalDateTime? = null
    private var message: String = ""
    private val domainEvents: MutableList<PaymentDomainEvent> = mutableListOf()
    
    companion object {
        /**
         * 새로운 결제 생성
         */
        fun create(
            paymentId: String,
            reservationId: String,
            amount: BigDecimal,
            paymentMethod: String,
            customerName: String,
            customerEmail: String
        ): PaymentAggregate {
            val payment = PaymentAggregate()
            payment.paymentId = PaymentId.of(paymentId)
            payment.reservationId = ReservationId.of(reservationId)
            payment.amount = PaymentAmount.of(amount)
            payment.paymentMethod = PaymentMethod.of(paymentMethod)
            payment.customerInfo = CustomerInfo.of(customerName, customerEmail)
            payment.status = PaymentStatus.PENDING
            payment.processedAt = LocalDateTime.now()
            payment.message = "결제 생성됨"
            
            payment.addDomainEvent(
                PaymentDomainEvent.PaymentCreated.of(
                    paymentId, reservationId, amount, paymentMethod, customerName, customerEmail
                )
            )
            
            return payment
        }
        
        /**
         * 기존 결제 재구성
         */
        fun reconstruct(
            paymentId: String,
            reservationId: String,
            amount: BigDecimal,
            paymentMethod: String,
            customerName: String,
            customerEmail: String,
            status: PaymentStatus,
            processedAt: LocalDateTime?,
            message: String
        ): PaymentAggregate {
            val payment = PaymentAggregate()
            payment.paymentId = PaymentId.of(paymentId)
            payment.reservationId = ReservationId.of(reservationId)
            payment.amount = PaymentAmount.of(amount)
            payment.paymentMethod = PaymentMethod.of(paymentMethod)
            payment.customerInfo = CustomerInfo.of(customerName, customerEmail)
            payment.status = status
            payment.processedAt = processedAt
            payment.message = message
            
            return payment
        }
    }
    
    /**
     * 결제 승인 처리
     */
    fun approve() {
        if (status != PaymentStatus.PENDING) {
            throw PaymentAlreadyProcessedException("Payment ${paymentId?.value} is already processed with status: $status")
        }
        
        status = PaymentStatus.SUCCESS
        message = "결제 승인됨"
        processedAt = LocalDateTime.now()
        
        addDomainEvent(
            PaymentDomainEvent.PaymentApproved.of(
                paymentId!!.value, reservationId!!.value, amount!!.value
            )
        )
    }
    
    /**
     * 결제 거절 처리
     */
    fun reject(reason: String) {
        if (status != PaymentStatus.PENDING) {
            throw PaymentAlreadyProcessedException("Payment ${paymentId?.value} is already processed with status: $status")
        }
        
        status = PaymentStatus.FAILED
        message = "결제 거절됨: $reason"
        processedAt = LocalDateTime.now()
        
        addDomainEvent(
            PaymentDomainEvent.PaymentRejected.of(
                paymentId!!.value, reservationId!!.value, reason
            )
        )
    }
    
    /**
     * 결제 취소
     */
    fun cancel() {
        if (status != PaymentStatus.SUCCESS) {
            throw InvalidPaymentOperationException("Only successful payments can be cancelled. Current status: $status")
        }
        
        status = PaymentStatus.CANCELLED
        message = "결제 취소됨"
        
        addDomainEvent(
            PaymentDomainEvent.PaymentCancelled.of(
                paymentId!!.value, reservationId!!.value, amount!!.value
            )
        )
    }
    
    /**
     * 결제 취소 가능 여부 확인
     */
    fun canBeCancelled(): Boolean = status == PaymentStatus.SUCCESS
    
    /**
     * 결제 성공 여부 확인
     */
    fun isSuccessful(): Boolean = status == PaymentStatus.SUCCESS
    
    /**
     * 결제 실패 여부 확인
     */
    fun isFailed(): Boolean = status == PaymentStatus.FAILED
    
    /**
     * 결제 대기 여부 확인
     */
    fun isPending(): Boolean = status == PaymentStatus.PENDING
    
    /**
     * 도메인 이벤트 추가
     */
    private fun addDomainEvent(event: PaymentDomainEvent) {
        domainEvents.add(event)
    }
    
    /**
     * 도메인 이벤트 목록 반환
     */
    fun getDomainEvents(): List<PaymentDomainEvent> = domainEvents.toList()
    
    /**
     * 도메인 이벤트 초기화
     */
    fun clearDomainEvents() {
        domainEvents.clear()
    }
    
    // Getters
    fun getPaymentId(): String = paymentId?.value ?: ""
    fun getReservationId(): String = reservationId?.value ?: ""
    fun getAmount(): BigDecimal = amount?.value ?: BigDecimal.ZERO
    fun getPaymentMethod(): String = paymentMethod?.method ?: ""
    fun getCustomerName(): String = customerInfo?.name ?: ""
    fun getCustomerEmail(): String = customerInfo?.email ?: ""
    fun getStatus(): PaymentStatus = status
    fun getProcessedAt(): LocalDateTime? = processedAt
    fun getMessage(): String = message
    fun getPaymentInfo(): PaymentInfo = PaymentInfo.of(paymentId!!.value, amount!!.value, status, processedAt)
}