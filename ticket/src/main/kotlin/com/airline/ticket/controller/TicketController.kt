package com.airline.ticket.controller

import com.airline.ticket.dto.TicketRequest
import com.airline.ticket.dto.TicketResponse
import com.airline.ticket.service.TicketService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/tickets")
class TicketController(
    private val ticketService: TicketService
) {
    
    @PostMapping
    fun issueTicket(@RequestBody request: TicketRequest): ResponseEntity<TicketResponse> {
        val response = ticketService.issueTicket(request)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/{ticketId}")
    fun getTicketById(@PathVariable ticketId: String): ResponseEntity<TicketResponse> {
        val ticket = ticketService.getTicketById(ticketId)
        return if (ticket != null) {
            ResponseEntity.ok(ticket)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{ticketId}/cancel")
    fun cancelTicket(@PathVariable ticketId: String): ResponseEntity<TicketResponse> {
        val cancelledTicket = ticketService.cancelTicket(ticketId)
        return if (cancelledTicket != null) {
            ResponseEntity.ok(cancelledTicket)
        } else {
            ResponseEntity.badRequest().build()
        }
    }
}