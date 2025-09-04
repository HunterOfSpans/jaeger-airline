package com.airline.reservation.api

import com.airline.reservation.service.FeignTracingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * OpenFeign 기반 동기 분산 추적 테스트 컨트롤러
 * 
 * 목적: OpenFeign을 통한 서비스 간 동기 호출의 분산 추적 검증
 * - Reservation → Flight → Payment → Ticket (동기 호출 체인)
 * - 각 서비스 호출이 OpenTelemetry를 통해 연결된 단일 트레이스로 추적됨
 * - Circuit Breaker, Retry 등 Resilience 패턴 적용
 * 
 * Jaeger UI에서 확인:
 * - Service: reservation-service
 * - Operation: POST /v1/tracing/feign/simple-flow
 */
@RestController
@RequestMapping("/v1/tracing/feign")
class FeignTracingController(
    private val feignTracingService: FeignTracingService
) {
    
    /**
     * 간단한 OpenFeign 동기 호출 체인 테스트
     * 
     * 플로우: Reservation → Flight (조회) → Payment (결제) → Ticket (발급)
     * 모든 호출이 동기적으로 실행되며 하나의 트레이스로 연결됨
     */
    @PostMapping("/simple-flow")
    fun testSimpleFeignFlow(): ResponseEntity<Map<String, Any>> {
        val result = feignTracingService.executeSimpleFeignFlow()
        return ResponseEntity.ok(result)
    }
    
    /**
     * 복잡한 OpenFeign 동기 호출 체인 테스트 (Circuit Breaker 포함)
     * 
     * 플로우: 전체 예약 프로세스를 OpenFeign으로만 처리
     * - 좌석 조회 → 좌석 예약 → 결제 처리 → 티켓 발급
     * - 실패 시 Circuit Breaker 동작 및 보상 트랜잭션 실행
     */
    @PostMapping("/complex-flow")
    fun testComplexFeignFlow(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val flightId = request["flightId"] as? String ?: "KE001"
        val passengerName = request["passengerName"] as? String ?: "Test User"
        
        val result = feignTracingService.executeComplexFeignFlow(flightId, passengerName)
        return ResponseEntity.ok(result)
    }
    
    /**
     * Circuit Breaker 동작 테스트
     * 
     * 의도적으로 존재하지 않는 항공편 ID로 호출하여 Circuit Breaker 작동 확인
     */
    @PostMapping("/circuit-breaker-test")
    fun testCircuitBreaker(): ResponseEntity<Map<String, Any>> {
        val result = feignTracingService.testCircuitBreakerFlow()
        return ResponseEntity.ok(result)
    }
    
    /**
     * 병렬 OpenFeign 호출 테스트
     * 
     * 여러 서비스를 동시에 호출하여 분산 추적에서 병렬 실행 확인
     */
    @PostMapping("/parallel-calls")
    fun testParallelFeignCalls(): ResponseEntity<Map<String, Any>> {
        val result = feignTracingService.executeParallelFeignCalls()
        return ResponseEntity.ok(result)
    }
}