package com.airline.reservation.dto.external

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentRequest(
    val reservationId: String,
    val amount: BigDecimal,
    val paymentMethod: String,
    val customerInfo: CustomerInfo
)

data class PaymentResponse(
    val paymentId: String,
    val status: PaymentStatus,
    val amount: BigDecimal,
    val reservationId: String,
    val processedAt: LocalDateTime,
    val message: String
)

data class CustomerInfo(
    val name: String,
    val email: String,
    val cardNumber: String? = null,
    val expiryMonth: String? = null,
    val expiryYear: String? = null
)

enum class PaymentStatus {
    SUCCESS, FAILED, PENDING, CANCELLED
}