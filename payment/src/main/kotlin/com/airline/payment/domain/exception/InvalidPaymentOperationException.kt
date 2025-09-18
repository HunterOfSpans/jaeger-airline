package com.airline.payment.domain.exception

/**
 * Domain Exception for invalid payment operations
 */
class InvalidPaymentOperationException(message: String) : RuntimeException(message) {
    constructor(message: String, cause: Throwable) : this(message)
}