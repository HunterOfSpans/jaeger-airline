package com.airline.ticket.repository

import com.airline.ticket.entity.Ticket
import com.airline.ticket.dto.TicketStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * 티켓 데이터 접근 계층
 * 
 * 티켓 정보의 CRUD 작업과 비즈니스 로직에 특화된 조회 기능을 제공합니다.
 * ConcurrentHashMap을 사용한 인메모리 저장소로 구현되어 있으며,
 * 실제 프로덕션에서는 JPA Repository로 교체 가능한 구조입니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Repository
class TicketRepository {
    
    private val tickets = ConcurrentHashMap<String, Ticket>()
    
    /**
     * 티켓 정보를 저장하거나 업데이트합니다.
     * 
     * @param ticket 저장할 티켓 엔티티
     * @return 저장된 티켓 엔티티
     */
    fun save(ticket: Ticket): Ticket {
        tickets[ticket.ticketId] = ticket
        return ticket
    }
    
    /**
     * 티켓 ID로 특정 티켓을 조회합니다.
     * 
     * @param ticketId 티켓 식별자
     * @return 티켓 엔티티, 존재하지 않으면 null
     */
    fun findById(ticketId: String): Ticket? {
        return tickets[ticketId]
    }
    
    /**
     * 모든 티켓을 조회합니다.
     * 
     * @return 전체 티켓 목록
     */
    fun findAll(): List<Ticket> {
        return tickets.values.toList()
    }
    
    /**
     * 예약 ID로 관련된 모든 티켓을 조회합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 해당 예약의 티켓 목록
     */
    fun findAllByReservationId(reservationId: String): List<Ticket> {
        return tickets.values.filter { it.reservationId == reservationId }
    }
    
    /**
     * 항공편 ID로 관련된 모든 티켓을 조회합니다.
     * 
     * @param flightId 항공편 식별자
     * @return 해당 항공편의 티켓 목록
     */
    fun findByFlightId(flightId: String): List<Ticket> {
        return tickets.values.filter { it.flightId == flightId }
    }
    
    /**
     * 티켓 상태로 티켓을 조회합니다.
     * 
     * @param status 티켓 상태 (ISSUED, CANCELLED 등)
     * @return 해당 상태의 티켓 목록
     */
    fun findByStatus(status: TicketStatus): List<Ticket> {
        return tickets.values.filter { it.status == status }
    }
    
    /**
     * 승객 이메일로 티켓을 조회합니다.
     * 
     * @param email 승객 이메일 주소
     * @return 해당 승객의 티켓 목록
     */
    fun findByPassengerEmail(email: String): List<Ticket> {
        return tickets.values.filter { it.passengerEmail == email }
    }

    
    /**
     * 예약 ID로 단일 티켓을 조회합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 해당 예약의 첫 번째 티켓, 존재하지 않으면 null
     */
    fun findByReservationId(reservationId: String): Ticket? {
        return tickets.values.firstOrNull { it.reservationId == reservationId }
    }
    
    /**
     * 항공편 ID와 좌석 번호로 티켓을 조회합니다.
     * 
     * @param flightId 항공편 식별자
     * @param seatNumber 좌석 번호
     * @return 해당 좌석의 티켓, 존재하지 않으면 null
     */
    fun findByFlightIdAndSeatNumber(flightId: String, seatNumber: String): Ticket? {
        return tickets.values.firstOrNull { 
            it.flightId == flightId && it.seatNumber == seatNumber 
        }
    }
    
    /**
     * 특정 티켓이 존재하는지 확인합니다.
     * 
     * @param ticketId 티켓 식별자
     * @return 존재 여부
     */
    fun existsById(ticketId: String): Boolean {
        return tickets.containsKey(ticketId)
    }
    
    /**
     * 특정 티켓을 삭제합니다.
     * 
     * @param ticketId 삭제할 티켓 식별자
     */
    fun deleteById(ticketId: String) {
        tickets.remove(ticketId)
    }
    
    /**
     * 저장된 티켓 총 개수를 반환합니다.
     * 
     * @return 전체 티켓 수
     */
    fun count(): Long {
        return tickets.size.toLong()
    }
    
    /**
     * 모든 티켓을 삭제합니다.
     */
    fun deleteAll() {
        tickets.clear()
    }
}