package com.airline.flight.controller;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestTraceController {
    
    private final Tracer tracer;
    
    public TestTraceController() {
        this.tracer = GlobalOpenTelemetry.get().getTracer("flight-test");
    }
    
    @GetMapping("/trace")
    public String testTrace() {
        Span span = tracer.spanBuilder("test-manual-span").startSpan();
        try {
            log.info("Manual trace test - span created: {}", span.getSpanContext().getSpanId());
            span.addEvent("Test trace event");
            span.setAttribute("test.attribute", "manual-test-value");
            return "Manual trace created: " + span.getSpanContext().getSpanId();
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/otel-status")
    public String getOtelStatus() {
        try {
            Tracer tracer = GlobalOpenTelemetry.get().getTracer("status-check");
            boolean isEnabled = tracer != null;
            log.info("OTEL Status - Tracer available: {}", isEnabled);
            return "OTEL Enabled: " + isEnabled + ", Tracer: " + tracer.getClass().getName();
        } catch (Exception e) {
            log.error("OTEL Status check failed", e);
            return "OTEL Error: " + e.getMessage();
        }
    }
}