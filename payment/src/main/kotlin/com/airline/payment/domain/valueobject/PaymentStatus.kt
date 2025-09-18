package com.airline.payment.domain.valueobject

/**
 * Payment Status Value Object
 */
enum class PaymentStatus {
    PENDING,    // 결제 대기
    SUCCESS,    // 결제 성공
    FAILED,     // 결제 실패
    CANCELLED;  // 결제 취소
    
    /**
     * 최종 상태 여부 확인
     */
    fun isFinalStatus(): Boolean = this in setOf(SUCCESS, FAILED, CANCELLED)
    
    /**
     * 활성 상태 여부 확인
     */
    fun isActiveStatus(): Boolean = this == SUCCESS
    
    /**
     * 실패 상태 여부 확인
     */
    fun isFailureStatus(): Boolean = this in setOf(FAILED, CANCELLED)
}