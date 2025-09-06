package com.airline.ticket.controller

import com.airline.ticket.dto.TicketRequest
import com.airline.ticket.dto.TicketResponse
import com.airline.ticket.service.TicketService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 티켓 관리 REST API 컨트롤러
 * 
 * 티켓 발행, 조회, 취소 등의 HTTP API를 제공합니다.
 * OpenTelemetry의 자동 계측을 통해 분산 추적을 지원하며,
 * RESTful API 설계 원칙을 따릅니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/tickets")
class TicketController(
    private val ticketService: TicketService
) {
    
    /**
     * 티켓을 발행합니다.
     * 
     * @param request 티켓 발행 요청 정보
     * @return 발행된 티켓 정보
     */
    @PostMapping
    fun issueTicket(@RequestBody request: TicketRequest): ResponseEntity<TicketResponse> {
        val response = ticketService.issueTicket(request)
        return ResponseEntity.ok(response)
    }
    
    /**
     * 티켓 ID로 특정 티켓 정보를 조회합니다.
     * 
     * @param ticketId 티켓 식별자
     * @return 티켓 정보, 존재하지 않으면 404 Not Found
     */
    @GetMapping("/{ticketId}")
    fun getTicketById(@PathVariable ticketId: String): ResponseEntity<TicketResponse> {
        val ticket = ticketService.getTicketById(ticketId)
        return if (ticket != null) {
            ResponseEntity.ok(ticket)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 티켓을 취소합니다.
     * 
     * @param ticketId 취소할 티켓 식별자
     * @return 취소된 티켓 정보, 실패 시 400 Bad Request
     */
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