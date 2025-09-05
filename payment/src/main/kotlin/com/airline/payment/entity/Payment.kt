package com.airline.payment.entity

import com.airline.payment.dto.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class Payment(
    val paymentId: String,
    var status: PaymentStatus,
    val amount: BigDecimal,
    val reservationId: String,
    val paymentMethod: String?,
    val customerName: String?,
    val customerEmail: String?,
    val processedAt: LocalDateTime,
    var message: String
)