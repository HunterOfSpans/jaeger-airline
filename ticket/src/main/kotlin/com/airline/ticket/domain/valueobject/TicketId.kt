package com.airline.ticket.domain.valueobject

/**
 * Ticket ID Value Object
 */
data class TicketId private constructor(val value: String) {
    
    companion object {
        fun of(value: String): TicketId {
            if (value.isBlank()) {
                throw IllegalArgumentException("Ticket ID cannot be blank")
            }
            
            return TicketId(value.trim())
        }
    }
    
    override fun toString(): String = value
}