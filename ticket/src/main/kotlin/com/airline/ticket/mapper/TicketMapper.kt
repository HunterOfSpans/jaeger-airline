package com.airline.ticket.mapper

import com.airline.ticket.dto.TicketRequest
import com.airline.ticket.dto.TicketResponse
import com.airline.ticket.dto.TicketStatus
import com.airline.ticket.entity.Ticket
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TicketMapper {
    
    fun toEntity(request: TicketRequest, ticketId: String, seatNumber: String): Ticket {
        return Ticket(
            ticketId = ticketId,
            status = TicketStatus.ISSUED,
            reservationId = request.reservationId,
            paymentId = request.paymentId,
            flightId = request.flightId,
            passengerName = request.passengerInfo?.name,
            passengerEmail = request.passengerInfo?.email,
            passengerPhone = request.passengerInfo?.phone,
            passportNumber = request.passengerInfo?.passportNumber,
            seatNumber = seatNumber,
            issuedAt = LocalDateTime.now(),
            message = "Ticket issued successfully"
        )
    }
    
    fun toResponse(ticket: Ticket): TicketResponse {
        return TicketResponse(
            ticketId = ticket.ticketId,
            status = ticket.status,
            reservationId = ticket.reservationId,
            paymentId = ticket.paymentId ?: "",
            flightId = ticket.flightId,
            passengerInfo = com.airline.ticket.dto.PassengerInfo(
                name = ticket.passengerName ?: "",
                email = ticket.passengerEmail ?: "",
                phone = ticket.passengerPhone ?: "",
                passportNumber = ticket.passportNumber
            ),
            seatNumber = ticket.seatNumber,
            issuedAt = ticket.issuedAt,
            message = ticket.message
        )
    }
    
    fun toResponseList(tickets: List<Ticket>): List<TicketResponse> {
        return tickets.map { toResponse(it) }
    }
}