package com.airline.payment.exception

/**
 * 결제 관련 예외 클래스들
 * 
 * @author Claude Code
 * @since 1.0
 */

sealed class PaymentException(message: String) : RuntimeException(message)

class PaymentNotFoundException(paymentId: String) : PaymentException(
    "Payment not found with ID: $paymentId"
)

class PaymentProcessingException(message: String) : PaymentException(
    "Payment processing failed: $message"
)

class InvalidPaymentRequestException(message: String) : PaymentException(
    "Invalid payment request: $message"
)

class PaymentAlreadyCancelledException(paymentId: String) : PaymentException(
    "Payment $paymentId is already cancelled"
)