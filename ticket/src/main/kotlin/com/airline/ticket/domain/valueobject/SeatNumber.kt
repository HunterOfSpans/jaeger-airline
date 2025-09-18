package com.airline.ticket.domain.valueobject

/**
 * Seat Number Value Object
 */
data class SeatNumber private constructor(val number: String) {
    
    companion object {
        private val SEAT_PATTERN = "^\\d{1,3}[A-F]$".toRegex()
        
        fun of(number: String): SeatNumber {
            if (number.isBlank()) {
                throw IllegalArgumentException("Seat number cannot be blank")
            }
            
            val normalizedNumber = number.trim().uppercase()
            if (!SEAT_PATTERN.matches(normalizedNumber)) {
                throw IllegalArgumentException("Invalid seat number format: $number. Expected format: 12A, 34B, etc.")
            }
            
            return SeatNumber(normalizedNumber)
        }
    }
    
    /**
     * 좌석 행 번호 반환
     */
    fun getRowNumber(): Int = number.dropLast(1).toInt()
    
    /**
     * 좌석 열 문자 반환
     */
    fun getSeatLetter(): Char = number.last()
    
    /**
     * 창가 좌석 여부 확인
     */
    fun isWindowSeat(): Boolean = getSeatLetter() in setOf('A', 'F')
    
    /**
     * 복도 좌석 여부 확인
     */
    fun isAisleSeat(): Boolean = getSeatLetter() in setOf('C', 'D')
    
    /**
     * 중간 좌석 여부 확인
     */
    fun isMiddleSeat(): Boolean = getSeatLetter() in setOf('B', 'E')
    
    override fun toString(): String = number
}