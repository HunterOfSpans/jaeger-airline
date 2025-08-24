package com.airline.reservation.client

import com.airline.reservation.dto.external.TicketRequest
import com.airline.reservation.dto.external.TicketResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(name = "ticket-service", url = "\${services.ticket.url:http://ticket:8081}")
interface TicketClient {
    
    @PostMapping("/v1/tickets")
    fun issueTicket(@RequestBody request: TicketRequest): TicketResponse
    
    @GetMapping("/v1/tickets/{ticketId}")
    fun getTicketById(@PathVariable ticketId: String): TicketResponse?
    
    @PostMapping("/v1/tickets/{ticketId}/cancel")
    fun cancelTicket(@PathVariable ticketId: String): TicketResponse?
}