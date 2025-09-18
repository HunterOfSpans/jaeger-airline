package com.airline.ticket.infrastructure.repository

import com.airline.ticket.domain.model.TicketAggregate
import com.airline.ticket.domain.repository.TicketDomainRepository
import com.airline.ticket.domain.valueobject.TicketId
import com.airline.ticket.domain.valueobject.ReservationId
import com.airline.ticket.domain.valueobject.TicketStatus
import com.airline.ticket.entity.Ticket
import com.airline.ticket.mapper.TicketMapper
import com.airline.ticket.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

/**
 * Ticket Domain Repository Implementation
 * 
 * 도메인 리포지토리와 인프라스트럭처 리포지토리 간의 어댑터
 */
@Repository
class TicketDomainRepositoryImpl(
    private val ticketRepository: TicketRepository,
    private val ticketMapper: TicketMapper
) : TicketDomainRepository {
    
    private val logger = LoggerFactory.getLogger(TicketDomainRepositoryImpl::class.java)
    
    override fun save(ticket: TicketAggregate): TicketAggregate {
        logger.debug("Saving ticket aggregate: {}", ticket.getTicketId())
        
        val entity = ticketMapper.toEntity(ticket)
        val savedEntity = ticketRepository.save(entity)
        
        return ticketMapper.toDomainAggregate(savedEntity)
    }
    
    override fun findById(ticketId: TicketId): TicketAggregate? {
        logger.debug("Finding ticket by ID: {}", ticketId.value)
        
        return ticketRepository.findById(ticketId.value)
            ?.let { ticketMapper.toDomainAggregate(it) }
    }
    
    override fun findByReservationId(reservationId: ReservationId): TicketAggregate? {
        logger.debug("Finding ticket by reservation ID: {}", reservationId.value)
        
        return ticketRepository.findByReservationId(reservationId.value)
            ?.let { ticketMapper.toDomainAggregate(it) }
    }
    
    override fun findByFlightId(flightId: String): List<TicketAggregate> {
        logger.debug("Finding tickets by flight ID: {}", flightId)
        
        return ticketRepository.findByFlightId(flightId)
            .map { ticketMapper.toDomainAggregate(it) }
    }
    
    override fun findByStatus(status: TicketStatus): List<TicketAggregate> {
        logger.debug("Finding tickets by status: {}", status)
        
        return ticketRepository.findByStatus(convertToEntityStatus(status))
            .map { ticketMapper.toDomainAggregate(it) }
    }
    
    override fun findByFlightIdAndSeatNumber(flightId: String, seatNumber: String): TicketAggregate? {
        logger.debug("Finding ticket by flight ID: {} and seat number: {}", flightId, seatNumber)
        
        return ticketRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
            ?.let { ticketMapper.toDomainAggregate(it) }
    }
    
    override fun delete(ticketId: TicketId) {
        logger.debug("Deleting ticket: {}", ticketId.value)
        ticketRepository.deleteById(ticketId.value)
    }
    
    override fun exists(ticketId: TicketId): Boolean {
        logger.debug("Checking if ticket exists: {}", ticketId.value)
        return ticketRepository.existsById(ticketId.value)
    }
    
    override fun findAll(): List<TicketAggregate> {
        logger.debug("Finding all tickets")
        
        return ticketRepository.findAll()
            .map { ticketMapper.toDomainAggregate(it) }
    }
    
    /**
     * 도메인 TicketStatus를 엔티티 TicketStatus로 변환
     */
    private fun convertToEntityStatus(domainStatus: TicketStatus): com.airline.ticket.dto.TicketStatus {
        return when (domainStatus) {
            TicketStatus.PENDING -> com.airline.ticket.dto.TicketStatus.PENDING
            TicketStatus.ISSUED -> com.airline.ticket.dto.TicketStatus.ISSUED
            TicketStatus.CANCELLED -> com.airline.ticket.dto.TicketStatus.CANCELLED
        }
    }
}