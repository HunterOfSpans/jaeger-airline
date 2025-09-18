package com.airline.ticket.domain.repository

import com.airline.ticket.domain.model.TicketAggregate
import com.airline.ticket.domain.valueobject.*

/**
 * Ticket Domain Repository Interface
 * 
 * DDD 패턴에 따른 도메인 리포지토리 인터페이스
 */
interface TicketDomainRepository {
    
    /**
     * 항공권 저장
     */
    fun save(ticket: TicketAggregate): TicketAggregate
    
    /**
     * ID로 항공권 조회
     */
    fun findById(ticketId: TicketId): TicketAggregate?
    
    /**
     * 예약 ID로 항공권 조회
     */
    fun findByReservationId(reservationId: ReservationId): TicketAggregate?
    
    /**
     * 항공편 ID로 항공권 목록 조회
     */
    fun findByFlightId(flightId: String): List<TicketAggregate>
    
    /**
     * 상태로 항공권 목록 조회
     */
    fun findByStatus(status: TicketStatus): List<TicketAggregate>
    
    /**
     * 좌석 번호로 항공권 조회
     */
    fun findByFlightIdAndSeatNumber(flightId: String, seatNumber: String): TicketAggregate?
    
    /**
     * 항공권 삭제
     */
    fun delete(ticketId: TicketId)
    
    /**
     * 항공권 존재 여부 확인
     */
    fun exists(ticketId: TicketId): Boolean
    
    /**
     * 모든 항공권 조회
     */
    fun findAll(): List<TicketAggregate>
}