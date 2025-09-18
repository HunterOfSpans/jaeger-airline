package com.airline.payment.domain.repository

import com.airline.payment.domain.model.PaymentAggregate
import com.airline.payment.domain.valueobject.PaymentId
import com.airline.payment.domain.valueobject.ReservationId
import com.airline.payment.domain.valueobject.PaymentStatus

/**
 * Payment Domain Repository Interface
 * 
 * DDD 패턴에 따른 도메인 리포지토리 인터페이스
 */
interface PaymentDomainRepository {
    
    /**
     * 결제 저장
     */
    fun save(payment: PaymentAggregate): PaymentAggregate
    
    /**
     * ID로 결제 조회
     */
    fun findById(paymentId: PaymentId): PaymentAggregate?
    
    /**
     * 예약 ID로 결제 조회
     */
    fun findByReservationId(reservationId: ReservationId): PaymentAggregate?
    
    /**
     * 상태로 결제 목록 조회
     */
    fun findByStatus(status: PaymentStatus): List<PaymentAggregate>
    
    /**
     * 결제 삭제
     */
    fun delete(paymentId: PaymentId)
    
    /**
     * 결제 존재 여부 확인
     */
    fun exists(paymentId: PaymentId): Boolean
    
    /**
     * 모든 결제 조회
     */
    fun findAll(): List<PaymentAggregate>
}