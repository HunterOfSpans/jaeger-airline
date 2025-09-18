package com.airline.ticket.domain.valueobject

/**
 * Passenger Info Value Object
 */
data class PassengerInfo private constructor(
    val name: String,
    val email: String,
    val phone: String?,
    val passportNumber: String?
) {
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        
        fun of(
            name: String, 
            email: String, 
            phone: String?, 
            passportNumber: String?
        ): PassengerInfo {
            if (name.isBlank()) {
                throw IllegalArgumentException("Passenger name cannot be blank")
            }
            
            if (email.isBlank()) {
                throw IllegalArgumentException("Passenger email cannot be blank")
            }
            
            if (!EMAIL_REGEX.matches(email)) {
                throw IllegalArgumentException("Invalid email format: $email")
            }
            
            return PassengerInfo(
                name.trim(), 
                email.trim().lowercase(), 
                phone?.trim(), 
                passportNumber?.trim()
            )
        }
    }
    
    /**
     * 승객 식별 정보 반환
     */
    fun getIdentifier(): String = "$name <$email>"
    
    /**
     * 여권 정보 포함 여부
     */
    fun hasPassport(): Boolean = !passportNumber.isNullOrBlank()
    
    /**
     * 연락처 정보 포함 여부
     */
    fun hasPhone(): Boolean = !phone.isNullOrBlank()
    
    override fun toString(): String = getIdentifier()
}