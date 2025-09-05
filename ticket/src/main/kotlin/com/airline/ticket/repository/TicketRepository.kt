package com.airline.ticket.repository

import com.airline.ticket.entity.Ticket
import com.airline.ticket.dto.TicketStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class TicketRepository {
    
    private val tickets = ConcurrentHashMap<String, Ticket>()
    
    fun save(ticket: Ticket): Ticket {
        tickets[ticket.ticketId] = ticket
        return ticket
    }
    
    fun findById(ticketId: String): Ticket? {
        return tickets[ticketId]
    }
    
    fun findAll(): List<Ticket> {
        return tickets.values.toList()
    }
    
    fun findByReservationId(reservationId: String): List<Ticket> {
        return tickets.values.filter { it.reservationId == reservationId }
    }
    
    fun findByFlightId(flightId: String): List<Ticket> {
        return tickets.values.filter { it.flightId == flightId }
    }
    
    fun findByStatus(status: TicketStatus): List<Ticket> {
        return tickets.values.filter { it.status == status }
    }
    
    fun findByPassengerEmail(email: String): List<Ticket> {
        return tickets.values.filter { it.passengerEmail == email }
    }
    
    fun existsById(ticketId: String): Boolean {
        return tickets.containsKey(ticketId)
    }
    
    fun deleteById(ticketId: String) {
        tickets.remove(ticketId)
    }
    
    fun count(): Long {
        return tickets.size.toLong()
    }
    
    fun deleteAll() {
        tickets.clear()
    }
}