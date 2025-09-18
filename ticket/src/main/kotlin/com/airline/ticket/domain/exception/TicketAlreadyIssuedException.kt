package com.airline.ticket.domain.exception

/**
 * Domain Exception for already issued tickets
 */
class TicketAlreadyIssuedException(message: String) : RuntimeException(message) {
    constructor(message: String, cause: Throwable) : this(message)
}