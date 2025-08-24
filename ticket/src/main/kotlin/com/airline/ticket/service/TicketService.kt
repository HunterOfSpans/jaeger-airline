package com.airline.ticket.service

import com.airline.ticket.dto.TicketRequest
import com.airline.ticket.dto.TicketResponse
import com.airline.ticket.dto.TicketStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class TicketService (
    private val kafkaTemplate: KafkaTemplate<String, String>
){
    private val logger = LoggerFactory.getLogger(TicketService::class.java)
    private val tickets = ConcurrentHashMap<String, TicketResponse>()
    
    fun issueTicket(request: TicketRequest): TicketResponse {
        logger.info("Issuing ticket for reservation: {}, flight: {}", 
                   request.reservationId, request.flightId)
        
        val ticketId = "TKT-${UUID.randomUUID().toString().take(8)}"
        val seatNumber = request.seatNumber ?: generateSeatNumber()
        
        val response = TicketResponse(
            ticketId = ticketId,
            status = TicketStatus.ISSUED,
            reservationId = request.reservationId,
            paymentId = request.paymentId,
            flightId = request.flightId,
            passengerInfo = request.passengerInfo,
            seatNumber = seatNumber,
            issuedAt = LocalDateTime.now(),
            message = "Ticket issued successfully"
        )
        
        // 티켓 정보 저장
        tickets[ticketId] = response
        
        // Kafka 이벤트 발송 (기존 로직)
        kafkaTemplate.send("ticket.issued", "Ticket issued for reservation: ${request.reservationId}")
        
        logger.info("Ticket issued: {} for seat {}", ticketId, seatNumber)
        return response
    }
    
    fun getTicketById(ticketId: String): TicketResponse? {
        return tickets[ticketId]
    }
    
    fun cancelTicket(ticketId: String): TicketResponse? {
        val ticket = tickets[ticketId]
        if (ticket != null && ticket.status == TicketStatus.ISSUED) {
            val cancelledTicket = ticket.copy(
                status = TicketStatus.CANCELLED,
                message = "Ticket cancelled"
            )
            tickets[ticketId] = cancelledTicket
            
            // 취소 이벤트 발송
            kafkaTemplate.send("ticket.cancelled", "Ticket cancelled: $ticketId")
            logger.info("Ticket cancelled: {}", ticketId)
            return cancelledTicket
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