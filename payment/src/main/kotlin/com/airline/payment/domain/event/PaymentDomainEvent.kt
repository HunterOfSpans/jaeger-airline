package com.airline.payment.domain.event

import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Payment Domain Events
 */
sealed class PaymentDomainEvent {
    abstract val eventId: String
    abstract val occurredOn: Instant
    
    /**
     * 결제 생성 이벤트
     */
    data class PaymentCreated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val paymentId: String,
        val reservationId: String,
        val amount: BigDecimal,
        val paymentMethod: String,
        val customerName: String,
        val customerEmail: String
    ) : PaymentDomainEvent() {
        
        companion object {
            fun of(
                paymentId: String,
                reservationId: String,
                amount: BigDecimal,
                paymentMethod: String,
                customerName: String,
                customerEmail: String
            ): PaymentCreated {
                return PaymentCreated(
                    paymentId = paymentId,
                    reservationId = reservationId,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    customerName = customerName,
                    customerEmail = customerEmail
                )
            }
        }
    }
    
    /**
     * 결제 승인 이벤트
     */
    data class PaymentApproved(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val paymentId: String,
        val reservationId: String,
        val amount: BigDecimal
    ) : PaymentDomainEvent() {
        
        companion object {
            fun of(
                paymentId: String,
                reservationId: String,
                amount: BigDecimal
            ): PaymentApproved {
                return PaymentApproved(
                    paymentId = paymentId,
                    reservationId = reservationId,
                    amount = amount
                )
            }
        }
    }
    
    /**
     * 결제 거절 이벤트
     */
    data class PaymentRejected(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val paymentId: String,
        val reservationId: String,
        val reason: String
    ) : PaymentDomainEvent() {
        
        companion object {
            fun of(
                paymentId: String,
                reservationId: String,
                reason: String
            ): PaymentRejected {
                return PaymentRejected(
                    paymentId = paymentId,
                    reservationId = reservationId,
                    reason = reason
                )
            }
        }
    }
    
    /**
     * 결제 취소 이벤트
     */
    data class PaymentCancelled(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val paymentId: String,
        val reservationId: String,
        val amount: BigDecimal
    ) : PaymentDomainEvent() {
        
        companion object {
            fun of(
                paymentId: String,
                reservationId: String,
                amount: BigDecimal
            ): PaymentCancelled {
                return PaymentCancelled(
                    paymentId = paymentId,
                    reservationId = reservationId,
                    amount = amount
                )
            }
        }
    }
}