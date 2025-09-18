package com.airline.ticket.mapper

import com.airline.ticket.domain.model.TicketAggregate
import com.airline.ticket.domain.valueobject.TicketStatus as DomainTicketStatus
import com.airline.ticket.dto.TicketRequest
import com.airline.ticket.dto.TicketResponse
import com.airline.ticket.dto.TicketStatus
import com.airline.ticket.entity.Ticket
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 티켓 데이터 변환 매퍼
 * 
 * Ticket 엔티티와 TicketResponse DTO 간의 변환을 담당합니다.
 * TicketRequest로부터 Ticket 엔티티를 생성하고,
 * Ticket 엔티티를 TicketResponse로 변환하는 기능을 제공합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Component
class TicketMapper {
    
    /**
     * TicketRequest로부터 Ticket 엔티티를 생성합니다.
     * 
     * @param request    티켓 발행 요청 DTO
     * @param ticketId   생성된 티켓 식별자
     * @param seatNumber 할당된 좌석 번호
     * @return 생성된 Ticket 엔티티 (초기 상태는 ISSUED)
     */
    fun toEntity(request: TicketRequest, ticketId: String, seatNumber: String): Ticket {
        return Ticket(
            ticketId = ticketId,
            status = TicketStatus.ISSUED,
            reservationId = request.reservationId,
            paymentId = request.paymentId,
            flightId = request.flightId,
            passengerName = request.passengerInfo?.name,
            passengerEmail = request.passengerInfo?.email,
            passengerPhone = request.passengerInfo?.phone,
            passportNumber = request.passengerInfo?.passportNumber,
            seatNumber = seatNumber,
            issuedAt = LocalDateTime.now(),
            message = "Ticket issued successfully"
        )
    }
    
    /**
     * Ticket 엔티티를 TicketResponse DTO로 변환합니다.
     * 
     * @param ticket 변환할 Ticket 엔티티
     * @return 변환된 TicketResponse DTO
     */
    fun toResponse(ticket: Ticket): TicketResponse {
        return TicketResponse(
            ticketId = ticket.ticketId,
            status = ticket.status,
            reservationId = ticket.reservationId,
            paymentId = ticket.paymentId ?: "",
            flightId = ticket.flightId,
            passengerInfo = com.airline.ticket.dto.PassengerInfo(
                name = ticket.passengerName ?: "",
                email = ticket.passengerEmail ?: "",
                phone = ticket.passengerPhone ?: "",
                passportNumber = ticket.passportNumber
            ),
            seatNumber = ticket.seatNumber,
            issuedAt = ticket.issuedAt,
            message = ticket.message
        )
    }
    
    /**
     * Ticket 엔티티 목록을 TicketResponse DTO 목록으로 변환합니다.
     * 
     * @param tickets 변환할 Ticket 엔티티 목록
     * @return 변환된 TicketResponse DTO 목록
     */
    fun toResponseList(tickets: List<Ticket>): List<TicketResponse> {
        return tickets.map { toResponse(it) }
    }

    
    /**
     * TicketAggregate를 Ticket 엔티티로 변환합니다.
     * 
     * @param aggregate 변환할 TicketAggregate
     * @return 변환된 Ticket 엔티티
     */
    fun toEntity(aggregate: TicketAggregate): Ticket {
        return Ticket(
            ticketId = aggregate.getTicketId(),
            status = convertToEntityStatus(aggregate.getStatus()),
            reservationId = aggregate.getReservationId(),
            paymentId = aggregate.getPaymentId(),
            flightId = aggregate.getFlightId(),
            passengerName = aggregate.getPassengerName(),
            passengerEmail = aggregate.getPassengerEmail(),
            passengerPhone = aggregate.getPassengerPhone(),
            passportNumber = aggregate.getPassportNumber(),
            seatNumber = aggregate.getSeatNumber(),
            issuedAt = aggregate.getIssuedAt() ?: LocalDateTime.now(),
            message = aggregate.getMessage()
        )
    }
    
    /**
     * Ticket 엔티티를 TicketAggregate로 변환합니다.
     * 
     * @param ticket 변환할 Ticket 엔티티
     * @return 변환된 TicketAggregate
     */
    fun toDomainAggregate(ticket: Ticket): TicketAggregate {
        return TicketAggregate.reconstruct(
            ticketId = ticket.ticketId,
            reservationId = ticket.reservationId,
            paymentId = ticket.paymentId,
            flightId = ticket.flightId,
            passengerName = ticket.passengerName ?: "",
            passengerEmail = ticket.passengerEmail ?: "",
            passengerPhone = ticket.passengerPhone,
            passportNumber = ticket.passportNumber,
            seatNumber = ticket.seatNumber,
            status = convertToDomainStatus(ticket.status),
            issuedAt = ticket.issuedAt,
            message = ticket.message
        )
    }
    
    /**
     * TicketAggregate를 TicketResponse로 변환합니다.
     * 
     * @param aggregate 변환할 TicketAggregate
     * @return 변환된 TicketResponse
     */
    fun toResponse(aggregate: TicketAggregate): TicketResponse {
        return TicketResponse(
            ticketId = aggregate.getTicketId(),
            status = convertToEntityStatus(aggregate.getStatus()),
            reservationId = aggregate.getReservationId(),
            paymentId = aggregate.getPaymentId() ?: "",
            flightId = aggregate.getFlightId(),
            passengerInfo = com.airline.ticket.dto.PassengerInfo(
                name = aggregate.getPassengerName(),
                email = aggregate.getPassengerEmail(),
                phone = aggregate.getPassengerPhone() ?: "",
                passportNumber = aggregate.getPassportNumber()
            ),
            seatNumber = aggregate.getSeatNumber(),
            issuedAt = aggregate.getIssuedAt(),
            message = aggregate.getMessage()
        )
    }
    
    /**
     * 도메인 TicketStatus를 엔티티 TicketStatus로 변환
     */
    private fun convertToEntityStatus(domainStatus: DomainTicketStatus): TicketStatus {
        return when (domainStatus) {
            DomainTicketStatus.PENDING -> TicketStatus.PENDING
            DomainTicketStatus.ISSUED -> TicketStatus.ISSUED
            DomainTicketStatus.CANCELLED -> TicketStatus.CANCELLED
        }
    }
    
    /**
     * 엔티티 TicketStatus를 도메인 TicketStatus로 변환
     */
    private fun convertToDomainStatus(entityStatus: TicketStatus): DomainTicketStatus {
        return when (entityStatus) {
            TicketStatus.PENDING -> DomainTicketStatus.PENDING
            TicketStatus.ISSUED -> DomainTicketStatus.ISSUED
            TicketStatus.CANCELLED -> DomainTicketStatus.CANCELLED
            TicketStatus.USED -> DomainTicketStatus.ISSUED
            TicketStatus.EXPIRED -> DomainTicketStatus.CANCELLED
        }
    }
}