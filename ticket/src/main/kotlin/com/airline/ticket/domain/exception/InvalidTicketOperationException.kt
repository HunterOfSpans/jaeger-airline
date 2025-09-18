package com.airline.ticket.domain.exception

/**
 * Domain Exception for invalid ticket operations
 */
class InvalidTicketOperationException(message: String) : RuntimeException(message) {
    constructor(message: String, cause: Throwable) : this(message)
}