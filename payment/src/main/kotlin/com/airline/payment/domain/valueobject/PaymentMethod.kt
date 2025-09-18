package com.airline.payment.domain.valueobject

/**
 * Payment Method Value Object
 */
data class PaymentMethod private constructor(val method: String) {
    
    companion object {
        private val VALID_METHODS = setOf("CARD", "BANK_TRANSFER", "DIGITAL_WALLET", "CASH")
        
        fun of(method: String): PaymentMethod {
            if (method.isBlank()) {
                throw IllegalArgumentException("Payment method cannot be blank")
            }
            
            val normalizedMethod = method.trim().uppercase()
            if (normalizedMethod !in VALID_METHODS) {
                throw IllegalArgumentException("Invalid payment method: $method. Valid methods: $VALID_METHODS")
            }
            
            return PaymentMethod(normalizedMethod)
        }
    }
    
    /**
     * 카드 결제 여부
     */
    fun isCard(): Boolean = method == "CARD"
    
    /**
     * 계좌이체 여부
     */
    fun isBankTransfer(): Boolean = method == "BANK_TRANSFER"
    
    /**
     * 디지털 지갑 여부
     */
    fun isDigitalWallet(): Boolean = method == "DIGITAL_WALLET"
    
    /**
     * 현금 결제 여부
     */
    fun isCash(): Boolean = method == "CASH"
    
    override fun toString(): String = method
}