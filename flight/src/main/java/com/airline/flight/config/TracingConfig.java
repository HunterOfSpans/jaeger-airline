package com.airline.flight.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flight Service - 분산 추적을 위한 OpenTelemetry 설정
 */
@Configuration
public class TracingConfig {
    
    @Autowired
    private OpenTelemetry openTelemetry;
    
    @Bean
    public Tracer tracer() {
        return openTelemetry.getTracer("flight-service", "1.0.0");
    }
}