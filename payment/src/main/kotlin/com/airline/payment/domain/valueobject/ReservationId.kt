package com.airline.payment.domain.valueobject

/**
 * Reservation ID Value Object
 */
data class ReservationId private constructor(val value: String) {
    
    companion object {
        fun of(value: String): ReservationId {
            if (value.isBlank()) {
                throw IllegalArgumentException("Reservation ID cannot be blank")
            }
            
            return ReservationId(value.trim())
        }
    }
    
    override fun toString(): String = value
}