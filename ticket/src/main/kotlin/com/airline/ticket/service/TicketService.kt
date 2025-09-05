package com.airline.ticket.service

import com.airline.ticket.dto.TicketRequest
import com.airline.ticket.dto.TicketResponse
import com.airline.ticket.dto.TicketStatus
import com.airline.ticket.entity.Ticket
import com.airline.ticket.mapper.TicketMapper
import com.airline.ticket.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class TicketService (
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val ticketRepository: TicketRepository,
    private val ticketMapper: TicketMapper
){
    private val logger = LoggerFactory.getLogger(TicketService::class.java)
    
    fun issueTicket(request: TicketRequest): TicketResponse {
        logger.info("Issuing ticket for reservation: {}, flight: {}", 
                   request.reservationId, request.flightId)
        
        val ticketId = "TKT-${UUID.randomUUID().toString().take(8)}"
        val seatNumber = request.seatNumber ?: generateSeatNumber()
        
        // 엔티티 생성 및 저장
        val ticket = ticketMapper.toEntity(request, ticketId, seatNumber)
        val savedTicket = ticketRepository.save(ticket)
        
        // Kafka 이벤트 발송 (기존 로직)
        kafkaTemplate.send("ticket.issued", "Ticket issued for reservation: ${request.reservationId}")
        
        logger.info("Ticket issued: {} for seat {}", ticketId, seatNumber)
        return ticketMapper.toResponse(savedTicket)
    }
    
    fun getTicketById(ticketId: String): TicketResponse? {
        val ticket = ticketRepository.findById(ticketId)
        return ticket?.let { ticketMapper.toResponse(it) }
    }
    
    fun cancelTicket(ticketId: String): TicketResponse? {
        val ticket = ticketRepository.findById(ticketId)
        
        if (ticket != null && ticket.status == TicketStatus.ISSUED) {
            ticket.status = TicketStatus.CANCELLED
            ticket.message = "Ticket cancelled"
            
            val cancelledTicket = ticketRepository.save(ticket)
            
            // 취소 이벤트 발송
            kafkaTemplate.send("ticket.cancelled", "Ticket cancelled: $ticketId")
            logger.info("Ticket cancelled: {}", ticketId)
            return ticketMapper.toResponse(cancelledTicket)
        }
        return null
    }
    
    private fun generateSeatNumber(): String {
        val rows = (1..30).random()
        val seats = listOf("A", "B", "C", "D", "E", "F")
        return "$rows${seats.random()}"
    }
    
    // 기존 메서드 호환성 유지
    fun issue(){
        kafkaTemplate.send("ticket.issued","A ticket issued")
    }
}