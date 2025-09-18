package com.airline.ticket.domain.model

import com.airline.ticket.domain.event.TicketDomainEvent
import com.airline.ticket.domain.exception.InvalidTicketOperationException
import com.airline.ticket.domain.exception.TicketAlreadyIssuedException
import com.airline.ticket.domain.valueobject.*
import java.time.LocalDateTime

/**
 * Ticket Aggregate Root
 * 
 * 항공권 도메인의 애그리게이트 루트
 * 항공권 발급, 취소, 상태 변경 등의 비즈니스 로직을 캡슐화
 */
class TicketAggregate private constructor() {
    
    private var ticketId: TicketId? = null
    private var reservationId: ReservationId? = null
    private var paymentId: PaymentId? = null
    private var flightInfo: FlightInfo? = null
    private var passengerInfo: PassengerInfo? = null
    private var seatNumber: SeatNumber? = null
    private var status: TicketStatus = TicketStatus.PENDING
    private var issuedAt: LocalDateTime? = null
    private var message: String = ""
    private val domainEvents: MutableList<TicketDomainEvent> = mutableListOf()
    
    companion object {
        /**
         * 새로운 항공권 생성
         */
        fun create(
            ticketId: String,
            reservationId: String,
            paymentId: String?,
            flightId: String,
            passengerName: String,
            passengerEmail: String,
            passengerPhone: String?,
            passportNumber: String?,
            seatNumber: String
        ): TicketAggregate {
            val ticket = TicketAggregate()
            ticket.ticketId = TicketId.of(ticketId)
            ticket.reservationId = ReservationId.of(reservationId)
            ticket.paymentId = paymentId?.let { PaymentId.of(it) }
            ticket.flightInfo = FlightInfo.of(flightId)
            ticket.passengerInfo = PassengerInfo.of(
                passengerName, passengerEmail, passengerPhone, passportNumber
            )
            ticket.seatNumber = SeatNumber.of(seatNumber)
            ticket.status = TicketStatus.PENDING
            ticket.issuedAt = LocalDateTime.now()
            ticket.message = "항공권 생성됨"
            
            ticket.addDomainEvent(
                TicketDomainEvent.TicketCreated.of(
                    ticketId, reservationId, flightId, passengerName, seatNumber
                )
            )
            
            return ticket
        }
        
        /**
         * 기존 항공권 재구성
         */
        fun reconstruct(
            ticketId: String,
            reservationId: String,
            paymentId: String?,
            flightId: String,
            passengerName: String,
            passengerEmail: String,
            passengerPhone: String?,
            passportNumber: String?,
            seatNumber: String,
            status: TicketStatus,
            issuedAt: LocalDateTime?,
            message: String
        ): TicketAggregate {
            val ticket = TicketAggregate()
            ticket.ticketId = TicketId.of(ticketId)
            ticket.reservationId = ReservationId.of(reservationId)
            ticket.paymentId = paymentId?.let { PaymentId.of(it) }
            ticket.flightInfo = FlightInfo.of(flightId)
            ticket.passengerInfo = PassengerInfo.of(
                passengerName, passengerEmail, passengerPhone, passportNumber
            )
            ticket.seatNumber = SeatNumber.of(seatNumber)
            ticket.status = status
            ticket.issuedAt = issuedAt
            ticket.message = message
            
            return ticket
        }
    }
    
    /**
     * 항공권 발급
     */
    fun issue() {
        if (status != TicketStatus.PENDING) {
            throw TicketAlreadyIssuedException("Ticket ${ticketId?.value} is already issued with status: $status")
        }
        
        status = TicketStatus.ISSUED
        message = "항공권 발급됨"
        issuedAt = LocalDateTime.now()
        
        addDomainEvent(
            TicketDomainEvent.TicketIssued.of(
                ticketId!!.value, reservationId!!.value, flightInfo!!.flightId, 
                passengerInfo!!.name, seatNumber!!.number
            )
        )
    }
    
    /**
     * 항공권 취소
     */
    fun cancel() {
        if (status != TicketStatus.ISSUED) {
            throw InvalidTicketOperationException("Only issued tickets can be cancelled. Current status: $status")
        }
        
        status = TicketStatus.CANCELLED
        message = "항공권 취소됨"
        
        addDomainEvent(
            TicketDomainEvent.TicketCancelled.of(
                ticketId!!.value, reservationId!!.value, flightInfo!!.flightId, seatNumber!!.number
            )
        )
    }
    
    /**
     * 좌석 번호 변경
     */
    fun changeSeat(newSeatNumber: String) {
        if (status != TicketStatus.ISSUED) {
            throw InvalidTicketOperationException("Can only change seat for issued tickets. Current status: $status")
        }
        
        val oldSeatNumber = seatNumber!!.number
        seatNumber = SeatNumber.of(newSeatNumber)
        message = "좌석 변경됨: $oldSeatNumber -> $newSeatNumber"
        
        addDomainEvent(
            TicketDomainEvent.SeatChanged.of(
                ticketId!!.value, flightInfo!!.flightId, oldSeatNumber, newSeatNumber
            )
        )
    }
    
    /**
     * 항공권 취소 가능 여부 확인
     */
    fun canBeCancelled(): Boolean = status == TicketStatus.ISSUED
    
    /**
     * 항공권 발급 여부 확인
     */
    fun isIssued(): Boolean = status == TicketStatus.ISSUED
    
    /**
     * 항공권 취소 여부 확인
     */
    fun isCancelled(): Boolean = status == TicketStatus.CANCELLED
    
    /**
     * 좌석 번호 변경 가능 여부 확인
     */
    fun canChangeSeat(): Boolean = status == TicketStatus.ISSUED
    
    /**
     * 도메인 이벤트 추가
     */
    private fun addDomainEvent(event: TicketDomainEvent) {
        domainEvents.add(event)
    }
    
    /**
     * 도메인 이벤트 목록 반환
     */
    fun getDomainEvents(): List<TicketDomainEvent> = domainEvents.toList()
    
    /**
     * 도메인 이벤트 초기화
     */
    fun clearDomainEvents() {
        domainEvents.clear()
    }
    
    // Getters
    fun getTicketId(): String = ticketId?.value ?: ""
    fun getReservationId(): String = reservationId?.value ?: ""
    fun getPaymentId(): String? = paymentId?.value
    fun getFlightId(): String = flightInfo?.flightId ?: ""
    fun getPassengerName(): String = passengerInfo?.name ?: ""
    fun getPassengerEmail(): String = passengerInfo?.email ?: ""
    fun getPassengerPhone(): String? = passengerInfo?.phone
    fun getPassportNumber(): String? = passengerInfo?.passportNumber
    fun getSeatNumber(): String = seatNumber?.number ?: ""
    fun getStatus(): TicketStatus = status
    fun getIssuedAt(): LocalDateTime? = issuedAt
    fun getMessage(): String = message
    fun getTicketInfo(): TicketInfo = TicketInfo.of(ticketId!!.value, flightInfo!!.flightId, status, issuedAt)
}