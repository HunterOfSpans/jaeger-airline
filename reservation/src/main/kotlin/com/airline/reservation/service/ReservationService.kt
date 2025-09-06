package com.airline.reservation.service

import com.airline.reservation.client.FlightClient
import com.airline.reservation.client.PaymentClient
import com.airline.reservation.client.TicketClient
import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.dto.ReservationStatus
import com.airline.reservation.dto.external.*
import com.airline.reservation.entity.Reservation
import com.airline.reservation.exception.*
import com.airline.reservation.mapper.ReservationMapper
import com.airline.reservation.repository.ReservationRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * 예약 관리 서비스 (오케스트레이션)
 * 
 * 항공권 예약의 전체 프로세스를 오케스트레이션하는 메인 서비스입니다.
 * Saga 패턴을 구현하여 분산 트랜잭션을 관리하며, 실패 시 보상 트랜잭션을 실행합니다.
 * Circuit Breaker 패턴으로 장애 복원력을 제공하고, 예약-항공편-결제-항공권 발급의
 * 전체 프로세스를 조율합니다. OpenFeign을 통한 동기 호출과 Kafka를 통한 이벤트 발행을 담당합니다.
 * 
 * @author Claude Code  
 * @since 1.0
 */
@Service
class ReservationService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val flightClient: FlightClient,
    private val paymentClient: PaymentClient,
    private val ticketClient: TicketClient,
    private val reservationRepository: ReservationRepository,
    private val reservationMapper: ReservationMapper
) {
    private val logger = LoggerFactory.getLogger(ReservationService::class.java)
    
    /**
     * 새로운 예약을 생성합니다.
     * 
     * 항공편 확인 → 좌석 예약 → 결제 처리 → 항공권 발급의 전체 프로세스를 수행합니다.
     * 각 단계에서 실패할 경우 이전 단계들을 롤백하는 보상 트랜잭션을 실행합니다.
     * Circuit Breaker가 적용되어 외부 시스템 장애 시 fallback 처리를 수행합니다.
     * 
     * @param request 예약 요청 정보 (항공편 ID, 승객 정보, 결제 방법 포함)
     * @return 예약 처리 결과 (예약 ID, 상태, 결제 ID, 항공권 ID 등)
     */
    @CircuitBreaker(name = "reservation", fallbackMethod = "createReservationFallback")
    fun createReservation(request: ReservationRequest): ReservationResponse {
        val reservationId = generateReservationId()
        logger.info("예약 프로세스 시작: {} (항공편: {})", reservationId, request.flightId)
        
        val reservation = initializeReservation(request, reservationId)
        
        // 1단계: 항공편 검증 및 좌석 가용성 확인
        val flight = validateFlightAndAvailability(request, reservation)
        
        // 2단계: 좌석 예약 처리  
        reserveFlightSeats(request.flightId)
        
        // 3단계: 결제 처리
        val payment = processPayment(reservation, flight, request)
        
        // 4단계: 항공권 발급
        val ticket = issueTicket(reservation, payment, request)
        
        // 5단계: 예약 완료 처리
        return completeReservation(reservation, payment, ticket, flight)
    }
    
    /**
     * 예약 ID로 예약 정보를 조회합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 예약 정보 DTO, 존재하지 않으면 null
     */
    fun getReservationById(reservationId: String): ReservationResponse {
        logger.info("예약 정보 조회: {}", reservationId)
        
        if (reservationId.isBlank()) {
            throw InvalidReservationRequestException("Reservation ID cannot be blank")
        }
        
        val reservation = reservationRepository.findById(reservationId)
            ?: throw ReservationNotFoundException(reservationId)
            
        return reservationMapper.toResponse(reservation)
    }
    
    /**
     * 확정된 예약을 취소합니다.
     * 
     * 보상 트랜잭션을 실행하여 항공권 취소, 결제 취소, 좌석 해제를 순차적으로 수행합니다.
     * 확정 상태의 예약만 취소 가능하며, 취소 완료 시 상태를 변경합니다.
     * 
     * @param reservationId 취소할 예약 식별자  
     * @return 취소된 예약 정보, 취소 불가능하면 null
     */
    fun cancelReservation(reservationId: String): ReservationResponse? {
        logger.info("예약 취소 요청: {}", reservationId)
        val reservation = reservationRepository.findById(reservationId)
        
        if (canCancelReservation(reservation)) {
            executeFullCompensation(reservation!!)
            reservation.status = ReservationStatus.CANCELLED
            reservation.message = "예약 취소됨"
            
            val cancelledReservation = reservationRepository.save(reservation)
            logger.info("예약 취소 완료: {}", reservationId)
            return reservationMapper.toResponse(cancelledReservation)
        }
        return null
    }
    
    /**
     * Circuit Breaker 활성화 시 호출되는 fallback 메서드
     */
    fun createReservationFallback(request: ReservationRequest, ex: Exception): ReservationResponse {
        logger.error("Circuit Breaker 활성화 - 예약 서비스 일시 중단", ex)
        val reservationId = generateReservationId()
        val reservation = reservationMapper.toEntity(request, reservationId)
        return updateReservationToFailed(reservation, "서비스 일시 중단. 잠시 후 다시 시도해주세요.")
    }
    
    // ===== 메서드 추출 리팩토링 =====
    
    /**
     * 예약 ID를 생성합니다.
     */
    private fun generateReservationId(): String {
        return "RES-${UUID.randomUUID().toString().take(8)}"
    }
    
    /**
     * 초기 예약 엔티티를 생성하고 저장합니다.
     */
    private fun initializeReservation(request: ReservationRequest, reservationId: String): Reservation {
        val reservation = reservationMapper.toEntity(request, reservationId)
        return reservationRepository.save(reservation)
    }
    
    /**
     * 항공편 유효성과 좌석 가용성을 검증합니다.
     */
    private fun validateFlightAndAvailability(request: ReservationRequest, reservation: Reservation): FlightDto {
        val flight = flightClient.getFlightById(request.flightId)
            ?: throw RuntimeException("항공편을 찾을 수 없습니다: ${request.flightId}")
        
        val availabilityRequest = AvailabilityRequest(requestedSeats = 1)
        val availability = flightClient.checkAvailability(request.flightId, availabilityRequest)
        
        if (!availability.available) {
            throw RuntimeException("예약 가능한 좌석이 없습니다")
        }
        
        return flight
    }
    
    /**
     * 항공편 좌석을 예약합니다.
     */
    private fun reserveFlightSeats(flightId: String) {
        val availabilityRequest = AvailabilityRequest(requestedSeats = 1)
        flightClient.reserveSeats(flightId, availabilityRequest)
        logger.info("좌석 예약 완료: {}", flightId)
    }
    
    /**
     * 결제를 처리합니다.
     */
    private fun processPayment(reservation: Reservation, flight: FlightDto, request: ReservationRequest): PaymentResponse {
        val paymentRequest = PaymentRequest(
            reservationId = reservation.reservationId,
            amount = flight.price,
            paymentMethod = request.paymentMethod,
            customerInfo = CustomerInfo(
                name = request.passengerInfo.name,
                email = request.passengerInfo.email
            )
        )
        
        val paymentResponse = paymentClient.processPayment(paymentRequest)
        
        if (paymentResponse.status != PaymentStatus.SUCCESS) {
            releaseSeats(request.flightId, 1)
            throw RuntimeException("결제 실패: ${paymentResponse.message}")
        }
        
        logger.info("결제 처리 완료: {}", paymentResponse.paymentId)
        return paymentResponse
    }
    
    /**
     * 항공권을 발급합니다.
     */
    private fun issueTicket(reservation: Reservation, payment: PaymentResponse, request: ReservationRequest): TicketResponse {
        val ticketRequest = TicketRequest(
            reservationId = reservation.reservationId,
            paymentId = payment.paymentId,
            flightId = request.flightId,
            passengerInfo = TicketPassengerInfo(
                name = request.passengerInfo.name,
                email = request.passengerInfo.email,
                phone = request.passengerInfo.phone,
                passportNumber = request.passengerInfo.passportNumber
            )
        )
        
        val ticketResponse = ticketClient.issueTicket(ticketRequest)
        logger.info("항공권 발급 완료: {}", ticketResponse.ticketId)
        return ticketResponse
    }
    
    /**
     * 예약을 완료 상태로 업데이트합니다.
     */
    private fun completeReservation(
        reservation: Reservation, 
        payment: PaymentResponse, 
        ticket: TicketResponse, 
        flight: FlightDto
    ): ReservationResponse {
        reservation.status = ReservationStatus.CONFIRMED
        reservation.paymentId = payment.paymentId
        reservation.ticketId = ticket.ticketId
        reservation.totalAmount = flight.price
        reservation.seatNumber = ticket.seatNumber
        reservation.message = "예약 완료"
        
        val completedReservation = reservationRepository.save(reservation)
        
        // 예약 완료 이벤트 발행
        kafkaTemplate.send("reservation.created", "Reservation completed: ${reservation.reservationId}")
        
        logger.info("예약 완료: {}", reservation.reservationId)
        return reservationMapper.toResponse(completedReservation)
    }
    
    /**
     * 예약을 실패 상태로 업데이트합니다.
     */
    private fun updateReservationToFailed(reservation: Reservation, message: String): ReservationResponse {
        reservation.status = ReservationStatus.FAILED
        reservation.message = message
        val failedReservation = reservationRepository.save(reservation)
        return reservationMapper.toResponse(failedReservation)
    }
    
    /**
     * 예약 취소 가능 여부를 확인합니다.
     */
    private fun canCancelReservation(reservation: Reservation?): Boolean {
        return reservation != null && reservation.status == ReservationStatus.CONFIRMED
    }
    
    /**
     * 전체 보상 트랜잭션을 실행합니다.
     */
    private fun executeFullCompensation(reservation: Reservation) {
        executeCompensation(
            reservation.reservationId, 
            reservation.flightId, 
            reservation.paymentId, 
            reservation.ticketId
        )
    }
    
    /**
     * 보상 트랜잭션을 실행합니다.
     * 
     * 항공권 취소 → 결제 취소 → 좌석 해제 순서로 롤백을 수행합니다.
     * 각 단계별로 실패해도 다음 단계를 계속 진행하여 최대한 리소스를 정리합니다.
     */
    private fun executeCompensation(
        reservationId: String, 
        flightId: String, 
        paymentId: String? = null, 
        ticketId: String? = null
    ) {
        logger.info("보상 트랜잭션 실행: {}", reservationId)
        
        // 항공권 취소
        ticketId?.let { 
            ticketClient.cancelTicket(it)
            logger.info("항공권 취소 완료: {}", it)
        }
        
        // 결제 취소
        paymentId?.let {
            paymentClient.cancelPayment(it)
            logger.info("결제 취소 완료: {}", it)
        }
        
        // 좌석 해제
        releaseSeats(flightId, 1)
    }
    
    /**
     * 항공편 좌석을 해제합니다.
     */
    private fun releaseSeats(flightId: String, seats: Int) {
        val releaseRequest = AvailabilityRequest(requestedSeats = seats)
        flightClient.releaseSeats(flightId, releaseRequest)
        logger.info("좌석 해제 완료: {} ({}석)", flightId, seats)
    }
    
    // 기존 메서드 호환성 유지
    fun reserve() {
        kafkaTemplate.send("reservation.created", "A reservation is created")
    }

    fun confirm() {
        println("A reservation is confirmed")
    }
}