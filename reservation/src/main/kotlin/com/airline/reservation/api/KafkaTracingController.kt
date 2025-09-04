package com.airline.reservation.api

import com.airline.reservation.service.KafkaTracingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Kafka 기반 비동기 분산 추적 테스트 컨트롤러
 * 
 * 목적: Kafka 메시지를 통한 비동기 이벤트 기반 분산 추적 검증
 * - 이벤트 발행 → 메시지 전파 → 다른 서비스에서 처리 (비동기 체인)
 * - 각 이벤트가 OpenTelemetry를 통해 연결된 단일 트레이스로 추적됨
 * - Event Sourcing, CQRS 패턴 시뮬레이션
 * 
 * Jaeger UI에서 확인:
 * - Service: reservation-service, payment-service, ticket-service
 * - Operation: reservation.created, payment.approved, ticket.issued
 */
@RestController
@RequestMapping("/v1/tracing/kafka")
class KafkaTracingController(
    private val kafkaTracingService: KafkaTracingService
) {
    
    /**
     * 간단한 Kafka 이벤트 체인 테스트
     * 
     * 플로우: reservation.created → payment.approved → ticket.issued
     * 각 이벤트가 비동기로 처리되며 트레이스로 연결됨
     */
    @PostMapping("/simple-events")
    fun testSimpleKafkaEvents(): ResponseEntity<Map<String, Any>> {
        val eventId = kafkaTracingService.triggerSimpleEventChain()
        return ResponseEntity.ok(mapOf(
            "message" to "Kafka event chain triggered",
            "eventId" to eventId,
            "instruction" to "Check Jaeger UI for distributed trace across services"
        ))
    }
    
    /**
     * 복잡한 Kafka 이벤트 플로우 테스트
     * 
     * 플로우: 전체 예약 프로세스를 이벤트 기반으로 처리
     * - reservation.requested → seat.reserved → payment.processed → ticket.generated → reservation.completed
     */
    @PostMapping("/complex-events")
    fun testComplexKafkaEvents(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val flightId = request["flightId"] as? String ?: "OZ456"
        val passengerName = request["passengerName"] as? String ?: "Event User"
        
        val eventId = kafkaTracingService.triggerComplexEventFlow(flightId, passengerName)
        return ResponseEntity.ok(mapOf(
            "message" to "Complex Kafka event flow triggered",
            "eventId" to eventId,
            "flightId" to flightId,
            "passengerName" to passengerName,
            "instruction" to "Monitor all services in Jaeger UI for event propagation"
        ))
    }
    
    /**
     * 이벤트 실패 및 보상 트랜잭션 테스트
     * 
     * 의도적으로 실패하는 이벤트를 발생시켜 보상 이벤트 체인 확인
     * payment.failed → ticket.cancelled → seat.released → reservation.failed
     */
    @PostMapping("/failure-compensation")
    fun testEventFailureCompensation(): ResponseEntity<Map<String, Any>> {
        val eventId = kafkaTracingService.triggerFailureCompensationFlow()
        return ResponseEntity.ok(mapOf(
            "message" to "Failure and compensation event flow triggered",
            "eventId" to eventId,
            "instruction" to "Check Jaeger UI for compensation event traces"
        ))
    }
    
    /**
     * 다중 토픽 이벤트 발행 테스트
     * 
     * 하나의 요청에서 여러 토픽으로 동시 이벤트 발행
     * 각 토픽의 컨슈머가 독립적으로 처리하는 분산 추적 확인
     */
    @PostMapping("/multi-topic-events")
    fun testMultiTopicEvents(): ResponseEntity<Map<String, Any>> {
        val eventIds = kafkaTracingService.triggerMultiTopicEvents()
        return ResponseEntity.ok(mapOf(
            "message" to "Multiple topic events triggered simultaneously",
            "eventIds" to eventIds,
            "topics" to listOf("reservation.analytics", "payment.audit", "ticket.notification"),
            "instruction" to "Observe parallel event processing in Jaeger UI"
        ))
    }
    
    /**
     * 이벤트 처리 상태 조회
     * 
     * 발행된 이벤트의 처리 상태를 조회하여 비동기 처리 완료 여부 확인
     */
    @GetMapping("/event-status/{eventId}")
    fun getEventStatus(@PathVariable eventId: String): ResponseEntity<Map<String, Any>> {
        val status = kafkaTracingService.getEventProcessingStatus(eventId)
        return ResponseEntity.ok(status)
    }
}