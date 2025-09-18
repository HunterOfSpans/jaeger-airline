package com.airline.payment.domain.exception

/**
 * Domain Exception for already processed payments
 */
class PaymentAlreadyProcessedException(message: String) : RuntimeException(message) {
    constructor(message: String, cause: Throwable) : this(message)
}