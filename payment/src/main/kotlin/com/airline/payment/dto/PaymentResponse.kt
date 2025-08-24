package com.airline.payment.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentResponse(
    val paymentId: String,
    val status: PaymentStatus,
    val amount: BigDecimal,
    val reservationId: String,
    val processedAt: LocalDateTime,
    val message: String
)

enum class PaymentStatus {
    SUCCESS, FAILED, PENDING, CANCELLED
}