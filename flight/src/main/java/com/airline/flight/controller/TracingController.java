package com.airline.flight.controller;

import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Flight 서비스 분산 추적 테스트 컨트롤러
 * 
 * 목적: Flight 서비스 단독 및 다른 서비스와의 연계 추적 테스트
 */
@Slf4j
@RestController
@RequestMapping("/v1/tracing")
@RequiredArgsConstructor
public class TracingController {
    
    private final Tracer tracer;
    
    /**
     * Flight 서비스 단독 추적 테스트
     */
    @PostMapping("/flight/simple")
    public ResponseEntity<Map<String, Object>> testSimpleFlightTracing() {
        String operationId = "FLIGHT-SIMPLE-" + UUID.randomUUID().toString().substring(0, 8);
        
        var span = tracer.spanBuilder("flight-simple-operation")
            .setAttribute("operation.id", operationId)
            .setAttribute("service", "flight")
            .startSpan();
            
        try {
            log.info("Starting simple flight tracing test with operationId: {}", operationId);
            
            // Flight 관련 비즈니스 로직 시뮬레이션
            Thread.sleep(100); // 처리 시간 시뮬레이션
            
            return ResponseEntity.ok(Map.of(
                "message", "Flight service tracing test completed",
                "operationId", operationId,
                "service", "flight",
                "traceId", span.getSpanContext().getTraceId(),
                "status", "success"
            ));
            
        } catch (Exception e) {
            span.recordException(e);
            log.error("Error in flight tracing test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "operationId", operationId,
                "status", "failed"
            ));
        } finally {
            span.end();
        }
    }
    
    /**
     * Flight 서비스 복잡한 작업 추적 테스트
     */
    @PostMapping("/flight/complex")
    public ResponseEntity<Map<String, Object>> testComplexFlightTracing(@RequestBody Map<String, Object> request) {
        String flightId = (String) request.getOrDefault("flightId", "KE001");
        String operationId = "FLIGHT-COMPLEX-" + UUID.randomUUID().toString().substring(0, 8);
        
        var span = tracer.spanBuilder("flight-complex-operation")
            .setAttribute("operation.id", operationId)
            .setAttribute("flight.id", flightId)
            .setAttribute("service", "flight")
            .startSpan();
            
        try {
            log.info("Starting complex flight tracing test for flightId: {}, operationId: {}", flightId, operationId);
            
            // 복잡한 Flight 비즈니스 로직 시뮬레이션
            var childSpan1 = tracer.spanBuilder("flight-data-lookup")
                .setAttribute("flight.id", flightId)
                .startSpan();
            try {
                Thread.sleep(50);
                log.info("Flight data lookup completed for: {}", flightId);
            } finally {
                childSpan1.end();
            }
            
            var childSpan2 = tracer.spanBuilder("seat-availability-check")
                .setAttribute("flight.id", flightId)
                .startSpan();
            try {
                Thread.sleep(30);
                log.info("Seat availability check completed for: {}", flightId);
            } finally {
                childSpan2.end();
            }
            
            var childSpan3 = tracer.spanBuilder("pricing-calculation")
                .setAttribute("flight.id", flightId)
                .startSpan();
            try {
                Thread.sleep(20);
                log.info("Pricing calculation completed for: {}", flightId);
            } finally {
                childSpan3.end();
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Complex flight operation completed",
                "operationId", operationId,
                "flightId", flightId,
                "service", "flight",
                "traceId", span.getSpanContext().getTraceId(),
                "operations", Map.of(
                    "dataLookup", "completed",
                    "availabilityCheck", "completed", 
                    "pricingCalculation", "completed"
                ),
                "status", "success"
            ));
            
        } catch (Exception e) {
            span.recordException(e);
            log.error("Error in complex flight tracing test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "operationId", operationId,
                "flightId", flightId,
                "status", "failed"
            ));
        } finally {
            span.end();
        }
    }
    
    /**
     * Flight 서비스 성능 테스트
     */
    @PostMapping("/flight/performance")
    public ResponseEntity<Map<String, Object>> testFlightPerformance() {
        String operationId = "FLIGHT-PERF-" + UUID.randomUUID().toString().substring(0, 8);
        
        var span = tracer.spanBuilder("flight-performance-test")
            .setAttribute("operation.id", operationId)
            .setAttribute("service", "flight")
            .startSpan();
            
        try {
            log.info("Starting flight performance test with operationId: {}", operationId);
            
            long startTime = System.currentTimeMillis();
            
            // 여러 병렬 작업 시뮬레이션
            for (int i = 1; i <= 5; i++) {
                var childSpan = tracer.spanBuilder("flight-operation-" + i)
                    .setAttribute("operation.sequence", i)
                    .startSpan();
                try {
                    Thread.sleep(10 + (i * 5)); // 가변 처리 시간
                } finally {
                    childSpan.end();
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            span.setAttribute("performance.duration_ms", duration);
            
            return ResponseEntity.ok(Map.of(
                "message", "Flight performance test completed",
                "operationId", operationId,
                "service", "flight",
                "traceId", span.getSpanContext().getTraceId(),
                "performance", Map.of(
                    "duration_ms", duration,
                    "operations_completed", 5,
                    "average_per_operation", duration / 5.0
                ),
                "status", "success"
            ));
            
        } catch (Exception e) {
            span.recordException(e);
            log.error("Error in flight performance test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "operationId", operationId,
                "status", "failed"
            ));
        } finally {
            span.end();
        }
    }
}