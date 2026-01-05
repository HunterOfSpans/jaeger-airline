package com.airline.payment.mapper

import com.airline.payment.domain.model.PaymentAggregate
import com.airline.payment.domain.valueobject.PaymentStatus as DomainPaymentStatus
import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.dto.PaymentStatus
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
     * PaymentAggregate를 Payment 엔티티로 변환합니다.
     * 
     * @param aggregate 변환할 PaymentAggregate
     * @return 변환된 Payment 엔티티
     */
    fun toEntity(aggregate: PaymentAggregate): Payment {
        return Payment(
            paymentId = aggregate.getPaymentId(),
            status = convertToEntityStatus(aggregate.getStatus()),
            amount = aggregate.getAmount(),
            reservationId = aggregate.getReservationId(),
            paymentMethod = aggregate.getPaymentMethod(),
            customerName = aggregate.getCustomerName(),
            customerEmail = aggregate.getCustomerEmail(),
            processedAt = aggregate.getProcessedAt() ?: java.time.LocalDateTime.now(),
            message = aggregate.getMessage()
        )
    }
    
    /**
     * Payment 엔티티를 PaymentAggregate로 변환합니다.
     * 
     * @param payment 변환할 Payment 엔티티
     * @return 변환된 PaymentAggregate
     */
    fun toDomainAggregate(payment: Payment): PaymentAggregate {
        return PaymentAggregate.reconstruct(
            paymentId = payment.paymentId,
            reservationId = payment.reservationId,
            amount = payment.amount,
            paymentMethod = payment.paymentMethod ?: "",
            customerName = payment.customerName ?: "",
            customerEmail = payment.customerEmail ?: "",
            status = convertToDomainStatus(payment.status),
            processedAt = payment.processedAt,
            message = payment.message
        )
    }
    
    /**
     * PaymentAggregate를 PaymentResponse로 변환합니다.
     * 
     * @param aggregate 변환할 PaymentAggregate
     * @return 변환된 PaymentResponse
     */
    fun toResponse(aggregate: PaymentAggregate): PaymentResponse {
        return PaymentResponse(
            paymentId = aggregate.getPaymentId(),
            status = convertToEntityStatus(aggregate.getStatus()),
            amount = aggregate.getAmount(),
            reservationId = aggregate.getReservationId(),
            processedAt = aggregate.getProcessedAt(),
            message = aggregate.getMessage()
        )
    }
    
    /**
     * 도메인 PaymentStatus를 엔티티 PaymentStatus로 변환
     */
    private fun convertToEntityStatus(domainStatus: DomainPaymentStatus): PaymentStatus {
        return when (domainStatus) {
            DomainPaymentStatus.PENDING -> PaymentStatus.PENDING
            DomainPaymentStatus.SUCCESS -> PaymentStatus.SUCCESS
            DomainPaymentStatus.FAILED -> PaymentStatus.FAILED
            DomainPaymentStatus.CANCELLED -> PaymentStatus.CANCELLED
        }
    }
    
    /**
     * 엔티티 PaymentStatus를 도메인 PaymentStatus로 변환
     */
    private fun convertToDomainStatus(entityStatus: PaymentStatus): DomainPaymentStatus {
        return when (entityStatus) {
            PaymentStatus.PENDING -> DomainPaymentStatus.PENDING
            PaymentStatus.SUCCESS -> DomainPaymentStatus.SUCCESS
            PaymentStatus.FAILED -> DomainPaymentStatus.FAILED
            PaymentStatus.CANCELLED -> DomainPaymentStatus.CANCELLED
        }
    }
}