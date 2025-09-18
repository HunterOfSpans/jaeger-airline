package com.airline.payment.domain.valueobject

/**
 * Customer Info Value Object
 */
data class CustomerInfo private constructor(
    val name: String,
    val email: String
) {
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        
        fun of(name: String, email: String): CustomerInfo {
            if (name.isBlank()) {
                throw IllegalArgumentException("Customer name cannot be blank")
            }
            
            if (email.isBlank()) {
                throw IllegalArgumentException("Customer email cannot be blank")
            }
            
            if (!EMAIL_REGEX.matches(email)) {
                throw IllegalArgumentException("Invalid email format: $email")
            }
            
            return CustomerInfo(name.trim(), email.trim().lowercase())
        }
    }
    
    /**
     * 고객 식별 정보 반환
     */
    fun getIdentifier(): String = "$name <$email>"
    
    override fun toString(): String = getIdentifier()
}