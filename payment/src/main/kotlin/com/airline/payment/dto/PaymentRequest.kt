package com.airline.payment.dto

import java.math.BigDecimal

data class PaymentRequest(
    val reservationId: String,
    val amount: BigDecimal,
    val paymentMethod: String,
    val customerInfo: CustomerInfo
)

data class CustomerInfo(
    val name: String,
    val email: String,
    val cardNumber: String? = null,
    val expiryMonth: String? = null,
    val expiryYear: String? = null
)