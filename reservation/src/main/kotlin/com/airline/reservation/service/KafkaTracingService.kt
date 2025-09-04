package com.airline.reservation.service

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Kafka 기반 비동기 분산 추적 서비스
 */
@Service
class KafkaTracingService(
    private val tracer: Tracer,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(KafkaTracingService::class.java)
    private val eventStatusMap = ConcurrentHashMap<String, Map<String, Any>>()
    
    /**
     * 간단한 Kafka 이벤트 체인 트리거
     */
    fun triggerSimpleEventChain(): String {
        val eventId = "EVENT-${UUID.randomUUID().toString().take(8)}"
        val span = tracer.spanBuilder("kafka-simple-event-chain")
            .setAttribute("event.id", eventId)
            .startSpan()
        
        try {
            logger.info("Starting simple Kafka event chain with eventId: {}", eventId)
            
            // 초기 상태 저장
            eventStatusMap[eventId] = mapOf(
                "eventId" to eventId,
                "status" to "started",
                "traceId" to span.spanContext.traceId,
                "startTime" to System.currentTimeMillis(),
                "events" to mutableListOf<String>()
            )
            
            // reservation.requested 이벤트 발행
            val eventMessage = mapOf(
                "eventId" to eventId,
                "type" to "SIMPLE_CHAIN",
                "reservationId" to "RES-KAFKA-$eventId",
                "flightId" to "KE001",
                "requestedSeats" to 1,
                "timestamp" to System.currentTimeMillis(),
                "message" to "Simple Kafka event chain started"
            ).toString()
            
            kafkaTemplate.send("reservation.requested", eventId, eventMessage)
            logger.info("Published reservation.requested event with eventId: {}", eventId)
            
            updateEventStatus(eventId, "reservation.requested", "published")
            
        } catch (e: Exception) {
            span.recordException(e)
            logger.error("Failed to trigger simple Kafka event chain", e)
            updateEventStatus(eventId, "error", e.message ?: "Unknown error")
        } finally {
            span.end()
        }
        
        return eventId
    }
    
    /**
     * 복잡한 Kafka 이벤트 플로우 트리거
     */
    fun triggerComplexEventFlow(flightId: String, passengerName: String): String {
        val eventId = "EVENT-COMPLEX-${UUID.randomUUID().toString().take(8)}"
        val span = tracer.spanBuilder("kafka-complex-event-flow")
            .setAttribute("event.id", eventId)
            .setAttribute("flight.id", flightId)
            .setAttribute("passenger.name", passengerName)
            .startSpan()
        
        try {
            logger.info("Starting complex Kafka event flow for flight: {}, passenger: {}", flightId, passengerName)
            
            // 초기 상태 저장
            eventStatusMap[eventId] = mapOf(
                "eventId" to eventId,
                "type" to "COMPLEX_FLOW",
                "flightId" to flightId,
                "passengerName" to passengerName,
                "status" to "started",
                "traceId" to span.spanContext.traceId,
                "startTime" to System.currentTimeMillis(),
                "events" to mutableListOf<String>()
            )
            
            // reservation.requested 이벤트 발행
            val eventMessage = mapOf(
                "eventId" to eventId,
                "type" to "RESERVATION_REQUESTED",
                "flightId" to flightId,
                "passengerName" to passengerName,
                "reservationId" to "RES-COMPLEX-$eventId",
                "requestedSeats" to 1,
                "timestamp" to System.currentTimeMillis(),
                "requiredSteps" to listOf("seat_reservation", "payment", "ticket_generation"),
                "message" to "Complex reservation flow requested"
            ).toString()
            
            kafkaTemplate.send("reservation.requested", eventId, eventMessage)
            logger.info("Published reservation.requested event with eventId: {}", eventId)
            
            updateEventStatus(eventId, "reservation.requested", "published")
            
        } catch (e: Exception) {
            span.recordException(e)
            logger.error("Failed to trigger complex Kafka event flow", e)
            updateEventStatus(eventId, "error", e.message ?: "Unknown error")
        } finally {
            span.end()
        }
        
        return eventId
    }
    
    /**
     * 실패 및 보상 트랜잭션 이벤트 플로우 트리거
     */
    fun triggerFailureCompensationFlow(): String {
        val eventId = "EVENT-FAIL-${UUID.randomUUID().toString().take(8)}"
        val span = tracer.spanBuilder("kafka-failure-compensation-flow")
            .setAttribute("event.id", eventId)
            .startSpan()
        
        try {
            logger.info("Starting failure compensation Kafka event flow with eventId: {}", eventId)
            
            // 초기 상태 저장
            eventStatusMap[eventId] = mapOf(
                "eventId" to eventId,
                "type" to "FAILURE_COMPENSATION",
                "status" to "started",
                "traceId" to span.spanContext.traceId,
                "startTime" to System.currentTimeMillis(),
                "events" to mutableListOf<String>()
            )
            
            // payment.failed 이벤트 발행
            val eventMessage = mapOf(
                "eventId" to eventId,
                "type" to "PAYMENT_FAILED",
                "reservationId" to "RES-FAIL-$eventId",
                "flightId" to "KE999",
                "reason" to "CARD_DECLINED",
                "timestamp" to System.currentTimeMillis(),
                "compensationRequired" to true,
                "message" to "Payment failed - compensation flow triggered"
            ).toString()
            
            kafkaTemplate.send("payment.failed", eventId, eventMessage)
            logger.info("Published payment.failed event with eventId: {}", eventId)
            
            updateEventStatus(eventId, "payment.failed", "published")
            
        } catch (e: Exception) {
            span.recordException(e)
            logger.error("Failed to trigger failure compensation flow", e)
            updateEventStatus(eventId, "error", e.message ?: "Unknown error")
        } finally {
            span.end()
        }
        
        return eventId
    }
    
    /**
     * 다중 토픽 이벤트 발행
     */
    fun triggerMultiTopicEvents(): List<String> {
        val baseEventId = "MULTI-${UUID.randomUUID().toString().take(8)}"
        val span = tracer.spanBuilder("kafka-multi-topic-events")
            .setAttribute("base.event.id", baseEventId)
            .startSpan()
        
        return try {
            logger.info("Starting multi-topic Kafka events with baseEventId: {}", baseEventId)
            
            val eventIds = mutableListOf<String>()
            
            // 여러 토픽에 동시 이벤트 발행
            val topics = mapOf(
                "reservation.analytics" to "ANALYTICS_DATA",
                "payment.audit" to "AUDIT_LOG",
                "ticket.notification" to "NOTIFICATION_REQUEST"
            )
            
            topics.forEach { (topic, eventType) ->
                val eventId = "$baseEventId-${topic.split('.').last().uppercase()}"
                eventIds.add(eventId)
                
                val eventMessage = mapOf(
                    "eventId" to eventId,
                    "baseEventId" to baseEventId,
                    "type" to eventType,
                    "topic" to topic,
                    "timestamp" to System.currentTimeMillis(),
                    "data" to mapOf(
                        "userId" to "user-123",
                        "sessionId" to "session-456",
                        "action" to "multi_topic_test"
                    )
                ).toString()
                
                kafkaTemplate.send(topic, eventId, eventMessage)
                logger.info("Published {} event to topic: {}", eventType, topic)
            }
            
            // 상태 저장
            eventStatusMap[baseEventId] = mapOf(
                "baseEventId" to baseEventId,
                "type" to "MULTI_TOPIC",
                "eventIds" to eventIds,
                "topics" to topics.keys.toList(),
                "status" to "published",
                "traceId" to span.spanContext.traceId,
                "startTime" to System.currentTimeMillis()
            )
            
            eventIds
            
        } catch (e: Exception) {
            span.recordException(e)
            logger.error("Failed to trigger multi-topic events", e)
            emptyList()
        } finally {
            span.end()
        }
    }
    
    /**
     * 이벤트 처리 상태 조회
     */
    fun getEventProcessingStatus(eventId: String): Map<String, Any> {
        val status = eventStatusMap[eventId]
        
        return if (status != null) {
            val currentTime = System.currentTimeMillis()
            val startTime = status["startTime"] as? Long ?: currentTime
            val duration = currentTime - startTime
            
            status.toMutableMap().apply {
                put("currentTime", currentTime)
                put("duration", duration)
                put("durationSeconds", duration / 1000.0)
            }
        } else {
            mapOf(
                "eventId" to eventId,
                "status" to "not_found",
                "message" to "Event not found or expired"
            )
        }
    }
    
    /**
     * 이벤트 상태 업데이트
     */
    private fun updateEventStatus(eventId: String, eventType: String, status: String) {
        eventStatusMap[eventId]?.let { currentStatus ->
            val events = (currentStatus["events"] as? MutableList<String>) ?: mutableListOf()
            events.add("$eventType:$status")
            
            val updatedStatus = currentStatus.toMutableMap().apply {
                put("events", events)
                put("lastEvent", eventType)
                put("lastStatus", status)
                put("lastUpdate", System.currentTimeMillis())
            }
            
            eventStatusMap[eventId] = updatedStatus
        }
    }
    
    /**
     * 이벤트 처리 완료 콜백
     */
    fun markEventProcessed(eventId: String, serviceName: String, eventType: String) {
        updateEventStatus(eventId, "$serviceName.$eventType", "processed")
        logger.info("Event {} processed by {} for eventType: {}", eventId, serviceName, eventType)
    }
}