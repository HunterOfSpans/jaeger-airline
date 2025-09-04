package com.airline.payment.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Payment Service - 분산 추적을 위한 OpenTelemetry 설정
 */
@Configuration
class TracingConfig {
    
    @Autowired
    private lateinit var openTelemetry: OpenTelemetry
    
    @Bean
    fun tracer(): Tracer {
        return openTelemetry.getTracer("payment-service", "1.0.0")
    }
}