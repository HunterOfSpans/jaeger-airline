package com.airline.payment.domain.valueobject

import java.math.BigDecimal

/**
 * Payment Amount Value Object
 */
data class PaymentAmount private constructor(val value: BigDecimal) {
    
    companion object {
        fun of(value: BigDecimal): PaymentAmount {
            if (value <= BigDecimal.ZERO) {
                throw IllegalArgumentException("Payment amount must be greater than 0")
            }
            
            if (value.scale() > 2) {
                throw IllegalArgumentException("Payment amount cannot have more than 2 decimal places")
            }
            
            return PaymentAmount(value)
        }
    }
    
    /**
     * 고액 결제 여부 확인 (100만원 이상)
     */
    fun isHighAmount(): Boolean = value >= BigDecimal("1000000")
    
    /**
     * 중간 금액 결제 여부 확인 (50만원 이상)
     */
    fun isMediumAmount(): Boolean = value >= BigDecimal("500000")
    
    /**
     * 소액 결제 여부 확인 (50만원 미만)
     */
    fun isLowAmount(): Boolean = value < BigDecimal("500000")
    
    override fun toString(): String = value.toString()
}