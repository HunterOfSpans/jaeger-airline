package com.airline.flight.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@Slf4j
public class OpenTelemetryConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void checkOpenTelemetryConfig() {
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        log.info("=== OpenTelemetry Configuration Check ===");
        log.info("OpenTelemetry instance: {}", openTelemetry.getClass().getName());
        log.info("Tracer provider: {}", openTelemetry.getTracerProvider().getClass().getName());
        
        // Environment variables check
        log.info("OTEL_SERVICE_NAME: {}", System.getenv("OTEL_SERVICE_NAME"));
        log.info("OTEL_EXPORTER_OTLP_ENDPOINT: {}", System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"));
        log.info("OTEL_TRACES_EXPORTER: {}", System.getenv("OTEL_TRACES_EXPORTER"));
        log.info("OTEL_TRACES_SAMPLER: {}", System.getenv("OTEL_TRACES_SAMPLER"));
        log.info("OTEL_INSTRUMENTATION_HTTP_ENABLED: {}", System.getenv("OTEL_INSTRUMENTATION_HTTP_ENABLED"));
        log.info("OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED: {}", System.getenv("OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED"));
        log.info("=== End OpenTelemetry Configuration ===");
    }
}