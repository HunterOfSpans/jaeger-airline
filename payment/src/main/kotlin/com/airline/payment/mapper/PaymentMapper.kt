package com.airline.payment.mapper

import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.entity.Payment
import org.springframework.stereotype.Component

@Component
class PaymentMapper {
    
    fun toEntity(request: PaymentRequest, paymentId: String): Payment {
        return Payment(
            paymentId = paymentId,
            status = com.airline.payment.dto.PaymentStatus.PENDING,
            amount = request.amount,
            reservationId = request.reservationId,
            paymentMethod = request.paymentMethod,
            customerName = request.customerInfo?.name,
            customerEmail = request.customerInfo?.email,
            processedAt = java.time.LocalDateTime.now(),
            message = "Payment initiated"
        )
    }
    
    fun toResponse(payment: Payment): PaymentResponse {
        return PaymentResponse(
            paymentId = payment.paymentId,
            status = payment.status,
            amount = payment.amount,
            reservationId = payment.reservationId,
            processedAt = payment.processedAt,
            message = payment.message
        )
    }
    
    fun toResponseList(payments: List<Payment>): List<PaymentResponse> {
        return payments.map { toResponse(it) }
    }
}