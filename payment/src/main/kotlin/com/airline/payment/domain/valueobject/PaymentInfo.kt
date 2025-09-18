package com.airline.payment.domain.valueobject

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Payment Info Value Object
 * 결제 정보 요약을 담는 값 객체
 */
data class PaymentInfo private constructor(
    val paymentId: String,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val processedAt: LocalDateTime?
) {
    
    companion object {
        fun of(
            paymentId: String,
            amount: BigDecimal,
            status: PaymentStatus,
            processedAt: LocalDateTime?
        ): PaymentInfo {
            if (paymentId.isBlank()) {
                throw IllegalArgumentException("Payment ID cannot be blank")
            }
            
            if (amount <= BigDecimal.ZERO) {
                throw IllegalArgumentException("Payment amount must be greater than 0")
            }
            
            return PaymentInfo(paymentId, amount, status, processedAt)
        }
    }
    
    /**
     * 결제 완료 여부
     */
    fun isCompleted(): Boolean = status == PaymentStatus.SUCCESS
    
    /**
     * 결제 실패 여부
     */
    fun isFailed(): Boolean = status.isFailureStatus()
    
    /**
     * 처리 시간 정보 반환
     */
    fun getProcessingInfo(): String {
        return when {
            processedAt != null -> "Processed at: $processedAt"
            else -> "Not yet processed"
        }
    }
}