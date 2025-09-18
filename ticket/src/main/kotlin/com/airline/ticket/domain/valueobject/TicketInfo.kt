package com.airline.ticket.domain.valueobject

import java.time.LocalDateTime

/**
 * Ticket Info Value Object
 * 항공권 정보 요약을 담는 값 객체
 */
data class TicketInfo private constructor(
    val ticketId: String,
    val flightId: String,
    val status: TicketStatus,
    val issuedAt: LocalDateTime?
) {
    
    companion object {
        fun of(
            ticketId: String,
            flightId: String,
            status: TicketStatus,
            issuedAt: LocalDateTime?
        ): TicketInfo {
            if (ticketId.isBlank()) {
                throw IllegalArgumentException("Ticket ID cannot be blank")
            }
            
            if (flightId.isBlank()) {
                throw IllegalArgumentException("Flight ID cannot be blank")
            }
            
            return TicketInfo(ticketId, flightId, status, issuedAt)
        }
    }
    
    /**
     * 항공권 발급 완료 여부
     */
    fun isIssued(): Boolean = status == TicketStatus.ISSUED
    
    /**
     * 항공권 취소 여부
     */
    fun isCancelled(): Boolean = status.isCancelledStatus()
    
    /**
     * 발급 시간 정보 반환
     */
    fun getIssuanceInfo(): String {
        return when {
            issuedAt != null -> "Issued at: $issuedAt"
            else -> "Not yet issued"
        }
    }
}