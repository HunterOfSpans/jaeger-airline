package com.airline.payment.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

/**
 * 결제 서비스 설정
 * 
 * 결제 성공률, 임계값 등의 비즈니스 로직 관련 설정을 외부화하여 관리합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
data class PaymentConfig(
    /**
     * 결제 성공률 설정
     */
    var successRates: SuccessRateConfig = SuccessRateConfig(),
    
    /**
     * 금액 임계값 설정  
     */
    var amountThresholds: AmountThresholdConfig = AmountThresholdConfig(),
    
    /**
     * ID 생성 설정
     */
    var idGeneration: IdGenerationConfig = IdGenerationConfig()
) {
    
    data class SuccessRateConfig(
        /**
         * 고액 결제 성공률 (기본값: 0.7 = 70%)
         */
        var highAmount: Double = 0.7,
        
        /**
         * 중간 금액 결제 성공률 (기본값: 0.9 = 90%)
         */
        var mediumAmount: Double = 0.9,
        
        /**
         * 소액 결제 성공률 (기본값: 0.95 = 95%)
         */
        var lowAmount: Double = 0.95
    )
    
    data class AmountThresholdConfig(
        /**
         * 고액 결제 임계값 (기본값: 100만원)
         */
        var high: BigDecimal = BigDecimal("1000000"),
        
        /**
         * 중간 금액 결제 임계값 (기본값: 50만원)
         */
        var medium: BigDecimal = BigDecimal("500000")
    )
    
    data class IdGenerationConfig(
        /**
         * 결제 ID 접두사 (기본값: "PAY-")
         */
        var prefix: String = "PAY-",
        
        /**
         * UUID 잘라내기 길이 (기본값: 8)
         */
        var uuidLength: Int = 8
    )
}