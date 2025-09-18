package com.airline.ticket.domain.valueobject

/**
 * Payment ID Value Object
 */
data class PaymentId private constructor(val value: String) {
    
    companion object {
        fun of(value: String): PaymentId {
            if (value.isBlank()) {
                throw IllegalArgumentException("Payment ID cannot be blank")
            }
            
            return PaymentId(value.trim())
        }
    }
    
    override fun toString(): String = value
}