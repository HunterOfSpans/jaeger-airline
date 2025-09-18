package com.airline.ticket.domain.valueobject

/**
 * Flight Info Value Object
 */
data class FlightInfo private constructor(val flightId: String) {
    
    companion object {
        fun of(flightId: String): FlightInfo {
            if (flightId.isBlank()) {
                throw IllegalArgumentException("Flight ID cannot be blank")
            }
            
            return FlightInfo(flightId.trim())
        }
    }
    
    override fun toString(): String = flightId
}