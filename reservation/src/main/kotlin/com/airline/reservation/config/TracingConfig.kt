package com.airline.reservation.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 분산 추적을 위한 OpenTelemetry 설정
 * 
 * Tracer Bean을 정의하여 수동 스팬 생성이 가능하도록 함
 * OpenTelemetry Spring Boot Starter가 자동 구성한 인스턴스를 사용
 */
@Configuration
class TracingConfig {
    
    @Autowired
    private lateinit var openTelemetry: OpenTelemetry
    
    /**
     * Tracer Bean 생성
     * 
     * 서비스에서 수동으로 스팬을 생성할 때 사용
     * instrumentationName은 스팬을 생성하는 라이브러리/서비스를 식별
     */
    @Bean
    fun tracer(): Tracer {
        return openTelemetry.getTracer("reservation-service", "1.0.0")
    }
}