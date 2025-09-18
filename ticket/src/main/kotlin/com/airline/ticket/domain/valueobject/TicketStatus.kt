package com.airline.ticket.domain.valueobject

/**
 * Ticket Status Value Object
 */
enum class TicketStatus {
    PENDING,    // 발급 대기
    ISSUED,     // 발급 완료
    CANCELLED;  // 취소됨
    
    /**
     * 최종 상태 여부 확인
     */
    fun isFinalStatus(): Boolean = this in setOf(ISSUED, CANCELLED)
    
    /**
     * 활성 상태 여부 확인
     */
    fun isActiveStatus(): Boolean = this == ISSUED
    
    /**
     * 취소 상태 여부 확인
     */
    fun isCancelledStatus(): Boolean = this == CANCELLED
}