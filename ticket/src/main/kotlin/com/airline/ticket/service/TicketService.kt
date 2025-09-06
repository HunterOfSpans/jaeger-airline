package com.airline.ticket.service

import com.airline.ticket.config.TicketConfig
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

/**
 * 항공권 관리 서비스
 * 
 * 항공권 발급, 취소, 상태 조회 등의 비즈니스 로직을 처리합니다.
 * 결제 완료 후 항공권을 자동 발급하며, 좌석 번호 자동 할당, 승객 정보 관리,
 * 취소 시 보상 트랜잭션 처리 등의 기능을 제공합니다.
 * Kafka 이벤트를 통한 비동기 처리와 분산 추적을 지원합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Service
class TicketService (
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val ticketRepository: TicketRepository,
    private val ticketMapper: TicketMapper,
    private val ticketConfig: TicketConfig
){
    private val logger = LoggerFactory.getLogger(TicketService::class.java)
    
    /**
     * 결제 완료된 예약에 대해 항공권을 발급합니다.
     * 
     * 좌석 번호가 지정되지 않은 경우 자동으로 할당하며, 발급 완료 시 
     * 이벤트를 발행하여 다른 서비스에 알립니다. 모든 승객 정보와 항공편 정보가
     * 포함된 완전한 항공권을 생성합니다.
     * 
     * @param request 항공권 발급 요청 (예약 ID, 결제 ID, 항공편 ID, 승객 정보 포함)
     * @return 발급된 항공권 정보 (항공권 ID, 좌석 번호, 발급 시간 등)
     */
    fun issueTicket(request: TicketRequest): TicketResponse {
        logger.info("예약 {}에 대한 항공권 발급 시작 (항공편: {})", request.reservationId, request.flightId)
        
        val ticketId = generateTicketId()
        val seatNumber = assignSeatNumber(request)
        
        // 항공권 엔티티 생성 및 저장
        val ticket = createTicketEntity(request, ticketId, seatNumber)
        val savedTicket = ticketRepository.save(ticket)
        
        // 항공권 발급 이벤트 발행
        publishTicketIssuedEvent(request.reservationId)
        
        logger.info("항공권 발급 완료: {} (좌석: {})", ticketId, seatNumber)
        return ticketMapper.toResponse(savedTicket)
    }
    
    /**
     * 항공권 ID로 항공권 정보를 조회합니다.
     * 
     * @param ticketId 항공권 식별자
     * @return 항공권 정보 DTO, 존재하지 않으면 null
     */
    fun getTicketById(ticketId: String): TicketResponse? {
        logger.info("항공권 정보 조회: {}", ticketId)
        val ticket = ticketRepository.findById(ticketId)
        return ticket?.let { ticketMapper.toResponse(it) }
    }
    
    /**
     * 발급된 항공권을 취소합니다.
     * 
     * 보상 트랜잭션의 일환으로 호출되며, 발급 상태의 항공권만 취소 가능합니다.
     * 취소 완료 시 취소 이벤트를 발행하여 좌석 해제 등의 후처리를 알립니다.
     * 
     * @param ticketId 취소할 항공권 식별자
     * @return 취소된 항공권 정보, 취소 불가능하면 null
     */
    fun cancelTicket(ticketId: String): TicketResponse? {
        logger.info("항공권 취소 요청: {}", ticketId)
        val ticket = ticketRepository.findById(ticketId)
        
        if (canCancelTicket(ticket)) {
            return processCancellation(ticket!!, ticketId)
        }
        
        logger.warn("취소할 수 없는 항공권: {} (상태: {})", ticketId, ticket?.status)
        return null
    }
    
    /**
     * 항공권 ID를 생성합니다.
     */
    private fun generateTicketId(): String {
        val idConfig = ticketConfig.idGeneration
        return "${idConfig.prefix}${UUID.randomUUID().toString().take(idConfig.uuidLength)}"
    }
    
    /**
     * 좌석 번호를 할당합니다.
     * 요청에 좌석이 지정되어 있으면 사용하고, 그렇지 않으면 자동 할당합니다.
     */
    private fun assignSeatNumber(request: TicketRequest): String {
        return request.seatNumber ?: generateSeatNumber()
    }
    
    /**
     * 항공권 엔티티를 생성합니다.
     */
    private fun createTicketEntity(request: TicketRequest, ticketId: String, seatNumber: String): Ticket {
        return ticketMapper.toEntity(request, ticketId, seatNumber)
    }
    
    /**
     * 항공권 발급 이벤트를 발행합니다.
     */
    private fun publishTicketIssuedEvent(reservationId: String) {
        kafkaTemplate.send("ticket.issued", "Ticket issued for reservation: $reservationId")
    }
    
    /**
     * 항공권 취소 가능 여부를 확인합니다.
     */
    private fun canCancelTicket(ticket: Ticket?): Boolean {
        return ticket != null && ticket.status == TicketStatus.ISSUED
    }
    
    /**
     * 항공권 취소 처리를 수행합니다.
     */
    private fun processCancellation(ticket: Ticket, ticketId: String): TicketResponse {
        ticket.status = TicketStatus.CANCELLED
        ticket.message = "항공권 취소됨"
        
        val cancelledTicket = ticketRepository.save(ticket)
        
        // 취소 이벤트 발행
        kafkaTemplate.send("ticket.cancelled", "Ticket cancelled: $ticketId")
        logger.info("항공권 취소 완료: {}", ticketId)
        
        return ticketMapper.toResponse(cancelledTicket)
    }
    
    /**
     * 랜덤한 좌석 번호를 생성합니다.
     * 
     * 1-30열의 A-F 좌석 중 하나를 무작위로 할당합니다.
     * 실제 시스템에서는 예약된 좌석을 제외하고 할당해야 합니다.
     */
    private fun generateSeatNumber(): String {
        val seatConfig = ticketConfig.seatAssignment
        val rows = (1..seatConfig.maxRows).random()
        val column = seatConfig.columns.random()
        return "$rows$column"
    }
    
    // 기존 메서드 호환성 유지
    fun issue(){
        kafkaTemplate.send("ticket.issued","A ticket issued")
    }
}