package com.airline.payment.mapper

import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.entity.Payment
import org.springframework.stereotype.Component

/**
 * 결제 데이터 변환 매퍼
 * 
 * Payment 엔티티와 PaymentResponse DTO 간의 변환을 담당합니다.
 * PaymentRequest로부터 Payment 엔티티를 생성하고,
 * Payment 엔티티를 PaymentResponse로 변환하는 기능을 제공합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Component
class PaymentMapper {
    
    /**
     * PaymentRequest로부터 Payment 엔티티를 생성합니다.
     * 
     * @param request   결제 요청 DTO
     * @param paymentId 생성된 결제 식별자
     * @return 생성된 Payment 엔티티 (초기 상태는 PENDING)
     */
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
    
    /**
     * Payment 엔티티를 PaymentResponse DTO로 변환합니다.
     * 
     * @param payment 변환할 Payment 엔티티
     * @return 변환된 PaymentResponse DTO
     */
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
    
    /**
     * Payment 엔티티 목록을 PaymentResponse DTO 목록으로 변환합니다.
     * 
     * @param payments 변환할 Payment 엔티티 목록
     * @return 변환된 PaymentResponse DTO 목록
     */
    fun toResponseList(payments: List<Payment>): List<PaymentResponse> {
        return payments.map { toResponse(it) }
    }
}