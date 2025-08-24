package com.airline.reservation.service

import com.airline.reservation.client.FlightClient
import com.airline.reservation.client.PaymentClient
import com.airline.reservation.client.TicketClient
import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.dto.ReservationStatus
import com.airline.reservation.dto.external.*
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class ReservationService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val flightClient: FlightClient,
    private val paymentClient: PaymentClient,
    private val ticketClient: TicketClient
) {
    private val logger = LoggerFactory.getLogger(ReservationService::class.java)
    private val reservations = ConcurrentHashMap<String, ReservationResponse>()
    
    @CircuitBreaker(name = "reservation", fallbackMethod = "createReservationFallback")
    fun createReservation(request: ReservationRequest): ReservationResponse {
        val reservationId = "RES-${UUID.randomUUID().toString().take(8)}"
        logger.info("Starting reservation process: {}", reservationId)
        
        try {
            // 1. 항공편 조회 및 좌석 확인
            val flight = flightClient.getFlightById(request.flightId)
                ?: throw RuntimeException("Flight not found: ${request.flightId}")
            
            val availabilityRequest = AvailabilityRequest(requestedSeats = 1)
            val availability = flightClient.checkAvailability(request.flightId, availabilityRequest)
            
            if (!availability.available) {
                return createFailedReservation(reservationId, request, "No seats available")
            }
            
            // 2. 좌석 예약
            flightClient.reserveSeats(request.flightId, availabilityRequest)
            logger.info("Seats reserved for flight: {}", request.flightId)
            
            // 3. 결제 처리
            val paymentRequest = PaymentRequest(
                reservationId = reservationId,
                amount = flight.price,
                paymentMethod = request.paymentMethod,
                customerInfo = CustomerInfo(
                    name = request.passengerInfo.name,
                    email = request.passengerInfo.email
                )
            )
            
            val paymentResponse = paymentClient.processPayment(paymentRequest)
            
            if (paymentResponse.status != PaymentStatus.SUCCESS) {
                // 결제 실패 시 좌석 해제
                releaseSeats(request.flightId, 1)
                return createFailedReservation(reservationId, request, "Payment failed: ${paymentResponse.message}")
            }
            
            logger.info("Payment successful: {}", paymentResponse.paymentId)
            
            // 4. 티켓 발급
            val ticketRequest = TicketRequest(
                reservationId = reservationId,
                paymentId = paymentResponse.paymentId,
                flightId = request.flightId,
                passengerInfo = TicketPassengerInfo(
                    name = request.passengerInfo.name,
                    email = request.passengerInfo.email,
                    phone = request.passengerInfo.phone,
                    passportNumber = request.passengerInfo.passportNumber
                )
            )
            
            val ticketResponse = ticketClient.issueTicket(ticketRequest)
            logger.info("Ticket issued: {}", ticketResponse.ticketId)
            
            // 5. 예약 완료
            val reservation = ReservationResponse(
                reservationId = reservationId,
                status = ReservationStatus.CONFIRMED,
                flightId = request.flightId,
                passengerInfo = request.passengerInfo,
                paymentId = paymentResponse.paymentId,
                ticketId = ticketResponse.ticketId,
                totalAmount = flight.price,
                seatNumber = ticketResponse.seatNumber,
                createdAt = LocalDateTime.now(),
                message = "Reservation completed successfully"
            )
            
            reservations[reservationId] = reservation
            
            // Kafka 이벤트 발송 (기존 로직 유지)
            kafkaTemplate.send("reservation.created", "Reservation completed: $reservationId")
            
            logger.info("Reservation completed successfully: {}", reservationId)
            return reservation
            
        } catch (e: Exception) {
            logger.error("Reservation failed: {}", e.message, e)
            // 보상 트랜잭션 실행
            executeCompensation(reservationId, request.flightId)
            return createFailedReservation(reservationId, request, "Reservation failed: ${e.message}")
        }
    }
    
    private fun createFailedReservation(reservationId: String, request: ReservationRequest, message: String): ReservationResponse {
        val reservation = ReservationResponse(
            reservationId = reservationId,
            status = ReservationStatus.FAILED,
            flightId = request.flightId,
            passengerInfo = request.passengerInfo,
            paymentId = null,
            ticketId = null,
            totalAmount = java.math.BigDecimal.ZERO,
            seatNumber = null,
            createdAt = LocalDateTime.now(),
            message = message
        )
        reservations[reservationId] = reservation
        return reservation
    }
    
    fun createReservationFallback(request: ReservationRequest, ex: Exception): ReservationResponse {
        logger.error("Circuit breaker activated for reservation", ex)
        val reservationId = "RES-${UUID.randomUUID().toString().take(8)}"
        return createFailedReservation(reservationId, request, "Service temporarily unavailable. Please try again later.")
    }
    
    fun getReservationById(reservationId: String): ReservationResponse? {
        return reservations[reservationId]
    }
    
    fun cancelReservation(reservationId: String): ReservationResponse? {
        val reservation = reservations[reservationId]
        if (reservation != null && reservation.status == ReservationStatus.CONFIRMED) {
            // 보상 트랜잭션 실행
            executeCompensation(reservationId, reservation.flightId, reservation.paymentId, reservation.ticketId)
            
            val cancelledReservation = reservation.copy(
                status = ReservationStatus.CANCELLED,
                message = "Reservation cancelled"
            )
            reservations[reservationId] = cancelledReservation
            logger.info("Reservation cancelled: {}", reservationId)
            return cancelledReservation
        }
        return null
    }
    
    private fun executeCompensation(reservationId: String, flightId: String, paymentId: String? = null, ticketId: String? = null) {
        logger.info("Executing compensation for reservation: {}", reservationId)
        
        // 티켓 취소
        if (ticketId != null) {
            try {
                ticketClient.cancelTicket(ticketId)
                logger.info("Ticket cancelled: {}", ticketId)
            } catch (e: Exception) {
                logger.error("Failed to cancel ticket: {}", ticketId, e)
            }
        }
        
        // 결제 취소
        if (paymentId != null) {
            try {
                paymentClient.cancelPayment(paymentId)
                logger.info("Payment cancelled: {}", paymentId)
            } catch (e: Exception) {
                logger.error("Failed to cancel payment: {}", paymentId, e)
            }
        }
        
        // 좌석 해제
        releaseSeats(flightId, 1)
    }
    
    private fun releaseSeats(flightId: String, seats: Int) {
        try {
            val releaseRequest = AvailabilityRequest(requestedSeats = seats)
            flightClient.releaseSeats(flightId, releaseRequest)
            logger.info("Seats released for flight: {}", flightId)
        } catch (e: Exception) {
            logger.error("Failed to release seats for flight: {}", flightId, e)
        }
    }
    
    // 기존 메서드 호호성 유지
    fun reserve() {
        kafkaTemplate.send("reservation.created", "A reservation is created")
    }

    fun confirm() {
        println("A reservation is confirmed")
    }
}