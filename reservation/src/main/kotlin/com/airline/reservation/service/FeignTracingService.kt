package com.airline.reservation.service

import com.airline.reservation.client.FlightClient
import com.airline.reservation.client.PaymentClient
import com.airline.reservation.client.TicketClient
import com.airline.reservation.dto.external.*
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

/**
 * OpenFeign 기반 동기 분산 추적 서비스
 */
@Service
class FeignTracingService(
    private val tracer: Tracer,
    private val flightClient: FlightClient,
    private val paymentClient: PaymentClient,
    private val ticketClient: TicketClient
) {
    private val logger = LoggerFactory.getLogger(FeignTracingService::class.java)
    
    /**
     * 간단한 OpenFeign 동기 호출 체인 (자동 instrumentation 테스트)
     */
    fun executeSimpleFeignFlow(): Map<String, Any> {
        logger.info("Starting simple Feign flow for distributed tracing (auto instrumentation)")
        
        val results = mutableMapOf<String, Any>()
        
        // 자동 instrumentation이 HTTP 요청을 자동으로 trace할 것임
        // 1. Flight Service 호출
        val flight = flightClient.getFlightById("KE001")
        results["flight"] = flight?.let { 
            mapOf("flightId" to it.flightId, "price" to it.price, "status" to "found")
        } ?: mapOf("status" to "not_found")
        
        // 2. Payment Service 호출  
        val paymentRequest = PaymentRequest(
            reservationId = "RES-AUTO-${UUID.randomUUID().toString().take(8)}",
            amount = flight?.price ?: BigDecimal("100000"),
            paymentMethod = "CARD",
            customerInfo = CustomerInfo(
                    name = "Tracing Test User",
                    email = "trace@test.com"
                )
            )
            val paymentResult = paymentClient.processPayment(paymentRequest)
            
            results["payment"] = mapOf(
                "paymentId" to paymentResult.paymentId,
                "status" to paymentResult.status.name,
                "amount" to paymentResult.amount
            )
            
            // 3. Ticket Service 호출
            val ticketRequest = TicketRequest(
                reservationId = paymentResult.reservationId,
                paymentId = paymentResult.paymentId,
                flightId = flight?.flightId ?: "KE001",
                passengerInfo = TicketPassengerInfo(
                    name = "Tracing Test User",
                    email = "trace@test.com",
                    phone = "010-1234-5678",
                    passportNumber = "M12345678"
                )
            )
            val ticketResult = ticketClient.issueTicket(ticketRequest)
            
            results["ticket"] = mapOf(
                "ticketId" to ticketResult.ticketId,
                "seatNumber" to ticketResult.seatNumber,
                "status" to "issued"
            )
            
        results["tracing"] = mapOf(
            "totalServices" to 3,
            "status" to "completed", 
            "message" to "All Feign calls completed successfully (auto instrumentation)"
        )
        
        logger.info("Simple Feign flow completed successfully")
        return results
    }
    
    /**
     * 복잡한 OpenFeign 동기 호출 체인
     */
    @CircuitBreaker(name = "feign-complex", fallbackMethod = "executeComplexFeignFlowFallback")
    fun executeComplexFeignFlow(flightId: String, passengerName: String): Map<String, Any> {
        logger.info("Starting complex Feign flow for flightId: {}, passenger: {} (auto instrumentation)", flightId, passengerName)
        
        val reservationId = "RES-COMPLEX-${UUID.randomUUID().toString().take(8)}"
        
        // 1. 항공편 조회
        val flight = flightClient.getFlightById(flightId)
            ?: throw RuntimeException("Flight not found: $flightId")
        
        // 2. 좌석 가용성 확인
        val availability = flightClient.checkAvailability(flightId, AvailabilityRequest(requestedSeats = 1))
        if (!availability.available) {
            throw RuntimeException("No seats available for flight: $flightId")
        }
        
        // 3. 좌석 예약
        flightClient.reserveSeats(flightId, AvailabilityRequest(requestedSeats = 1))
        
        // 4. 결제 처리
        val paymentRequest = PaymentRequest(
            reservationId = reservationId,
            amount = flight.price,
            paymentMethod = "CARD",
            customerInfo = CustomerInfo(name = passengerName, email = "$passengerName@test.com")
        )
        val paymentResult = paymentClient.processPayment(paymentRequest)
        
        // 5. 티켓 발급
        val ticketRequest = TicketRequest(
            reservationId = reservationId,
            paymentId = paymentResult.paymentId,
            flightId = flightId,
            passengerInfo = TicketPassengerInfo(
                name = passengerName,
                email = "$passengerName@test.com",
                phone = "010-1234-5678",
                passportNumber = "M${Random().nextInt(90000000) + 10000000}"
            )
        )
        val ticketResult = ticketClient.issueTicket(ticketRequest)
        
        logger.info("Complex Feign flow completed for reservationId: {}", reservationId)
        
        return mapOf(
            "reservationId" to reservationId,
            "flight" to mapOf("id" to flight.flightId, "price" to flight.price),
            "payment" to mapOf("id" to paymentResult.paymentId, "status" to paymentResult.status),
            "ticket" to mapOf("id" to ticketResult.ticketId, "seat" to ticketResult.seatNumber),
            "status" to "completed"
        )
    }
    
    /**
     * Circuit Breaker Fallback Method
     */
    fun executeComplexFeignFlowFallback(flightId: String, passengerName: String, ex: Exception): Map<String, Any> {
        logger.error("Circuit breaker activated for complex Feign flow", ex)
        return mapOf(
            "error" to "Circuit breaker activated - service temporarily unavailable",
            "flightId" to flightId,
            "passengerName" to passengerName,
            "status" to "circuit_breaker_open"
        )
    }
    
    /**
     * Circuit Breaker 테스트 플로우
     */
    fun testCircuitBreakerFlow(): Map<String, Any> {
        val result = executeComplexFeignFlow("NON_EXISTENT_FLIGHT", "Circuit Test User")
        return result.toMutableMap().apply { put("testType", "circuit_breaker") }
    }
    
    /**
     * 병렬 OpenFeign 호출 테스트
     */
    fun executeParallelFeignCalls(): Map<String, Any> {
        val results = mutableMapOf<String, Any>()
        
        // 여러 서비스를 동시에 호출
        val flight1 = flightClient.getFlightById("KE001")
            val flight2 = flightClient.getFlightById("OZ456")
            
        results["parallelResults"] = mapOf(
            "flight1" to (flight1?.flightId ?: "not_found"),
            "flight2" to (flight2?.flightId ?: "not_found"),
            "executionType" to "parallel",
            "status" to "completed"
        )
        
        return results
    }
}