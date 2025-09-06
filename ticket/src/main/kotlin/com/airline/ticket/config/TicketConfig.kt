package com.airline.ticket.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 티켓 서비스 설정
 * 
 * 좌석 할당, ID 생성 등의 비즈니스 로직 관련 설정을 외부화하여 관리합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "ticket")
data class TicketConfig(
    /**
     * 좌석 할당 설정
     */
    var seatAssignment: SeatAssignmentConfig = SeatAssignmentConfig(),
    
    /**
     * ID 생성 설정
     */
    var idGeneration: IdGenerationConfig = IdGenerationConfig()
) {
    
    data class SeatAssignmentConfig(
        /**
         * 최대 좌석 행 수 (기본값: 30)
         */
        var maxRows: Int = 30,
        
        /**
         * 좌석 열 (기본값: A-F)
         */
        var columns: List<String> = listOf("A", "B", "C", "D", "E", "F")
    )
    
    data class IdGenerationConfig(
        /**
         * 티켓 ID 접두사 (기본값: "TKT-")
         */
        var prefix: String = "TKT-",
        
        /**
         * UUID 잘라내기 길이 (기본값: 8)
         */
        var uuidLength: Int = 8
    )
}