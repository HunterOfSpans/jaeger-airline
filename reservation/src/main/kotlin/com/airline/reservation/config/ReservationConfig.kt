package com.airline.reservation.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

/**
 * 예약 서비스 설정
 * 
 * 예약 프로세스, 테스트 데이터 등의 비즈니스 로직 관련 설정을 외부화하여 관리합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "reservation")
data class ReservationConfig(
    /**
     * ID 생성 설정
     */
    var idGeneration: IdGenerationConfig = IdGenerationConfig(),
    
    /**
     * 테스트 데이터 설정
     */
    var testData: TestDataConfig = TestDataConfig()
) {
    
    data class IdGenerationConfig(
        /**
         * 예약 ID 접두사 (기본값: "RES-")
         */
        var prefix: String = "RES-",
        
        /**
         * 이벤트 ID 접두사 (기본값: "EVENT-")
         */
        var eventPrefix: String = "EVENT-",
        
        /**
         * UUID 잘라내기 길이 (기본값: 8)
         */
        var uuidLength: Int = 8
    )
    
    data class TestDataConfig(
        /**
         * 기본 항공편 ID
         */
        var defaultFlightId: String = "KE001",
        
        /**
         * 테스트용 항공편 ID 목록
         */
        var testFlightIds: List<String> = listOf("KE001", "OZ456", "KE999"),
        
        /**
         * 기본 결제 방법
         */
        var defaultPaymentMethod: String = "CARD",
        
        /**
         * 기본 결제 금액
         */
        var defaultAmount: BigDecimal = BigDecimal("100000"),
        
        /**
         * 기본 승객 정보
         */
        var defaultPassenger: PassengerInfo = PassengerInfo()
    ) {
        
        data class PassengerInfo(
            var name: String = "테스트 승객",
            var email: String = "test@example.com",
            var phone: String = "010-1234-5678",
            var passportPrefix: String = "M"
        )
    }
}