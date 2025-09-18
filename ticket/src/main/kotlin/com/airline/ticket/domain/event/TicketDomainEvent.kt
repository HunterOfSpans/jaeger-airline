package com.airline.ticket.domain.event

import java.time.Instant
import java.util.*

/**
 * Ticket Domain Events
 */
sealed class TicketDomainEvent {
    abstract val eventId: String
    abstract val occurredOn: Instant
    
    /**
     * 항공권 생성 이벤트
     */
    data class TicketCreated(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val ticketId: String,
        val reservationId: String,
        val flightId: String,
        val passengerName: String,
        val seatNumber: String
    ) : TicketDomainEvent() {
        
        companion object {
            fun of(
                ticketId: String,
                reservationId: String,
                flightId: String,
                passengerName: String,
                seatNumber: String
            ): TicketCreated {
                return TicketCreated(
                    ticketId = ticketId,
                    reservationId = reservationId,
                    flightId = flightId,
                    passengerName = passengerName,
                    seatNumber = seatNumber
                )
            }
        }
    }
    
    /**
     * 항공권 발급 이벤트
     */
    data class TicketIssued(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val ticketId: String,
        val reservationId: String,
        val flightId: String,
        val passengerName: String,
        val seatNumber: String
    ) : TicketDomainEvent() {
        
        companion object {
            fun of(
                ticketId: String,
                reservationId: String,
                flightId: String,
                passengerName: String,
                seatNumber: String
            ): TicketIssued {
                return TicketIssued(
                    ticketId = ticketId,
                    reservationId = reservationId,
                    flightId = flightId,
                    passengerName = passengerName,
                    seatNumber = seatNumber
                )
            }
        }
    }
    
    /**
     * 항공권 취소 이벤트
     */
    data class TicketCancelled(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val ticketId: String,
        val reservationId: String,
        val flightId: String,
        val seatNumber: String
    ) : TicketDomainEvent() {
        
        companion object {
            fun of(
                ticketId: String,
                reservationId: String,
                flightId: String,
                seatNumber: String
            ): TicketCancelled {
                return TicketCancelled(
                    ticketId = ticketId,
                    reservationId = reservationId,
                    flightId = flightId,
                    seatNumber = seatNumber
                )
            }
        }
    }
    
    /**
     * 좌석 변경 이벤트
     */
    data class SeatChanged(
        override val eventId: String = UUID.randomUUID().toString(),
        override val occurredOn: Instant = Instant.now(),
        val ticketId: String,
        val flightId: String,
        val oldSeatNumber: String,
        val newSeatNumber: String
    ) : TicketDomainEvent() {
        
        companion object {
            fun of(
                ticketId: String,
                flightId: String,
                oldSeatNumber: String,
                newSeatNumber: String
            ): SeatChanged {
                return SeatChanged(
                    ticketId = ticketId,
                    flightId = flightId,
                    oldSeatNumber = oldSeatNumber,
                    newSeatNumber = newSeatNumber
                )
            }
        }
    }
}