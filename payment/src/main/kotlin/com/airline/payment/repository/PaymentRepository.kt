package com.airline.payment.repository

import com.airline.payment.entity.Payment
import com.airline.payment.dto.PaymentStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * 결제 데이터 접근 계층
 * 
 * 결제 정보의 CRUD 작업과 비즈니스 로직에 특화된 조회 기능을 제공합니다.
 * ConcurrentHashMap을 사용한 인메모리 저장소로 구현되어 있으며,
 * 실제 프로덕션에서는 JPA Repository로 교체 가능한 구조입니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Repository
class PaymentRepository {
    
    private val payments = ConcurrentHashMap<String, Payment>()
    
    /**
     * 결제 정보를 저장하거나 업데이트합니다.
     * 
     * @param payment 저장할 결제 엔티티
     * @return 저장된 결제 엔티티
     */
    fun save(payment: Payment): Payment {
        payments[payment.paymentId] = payment
        return payment
    }
    
    /**
     * 결제 ID로 특정 결제를 조회합니다.
     * 
     * @param paymentId 결제 식별자
     * @return 결제 엔티티, 존재하지 않으면 null
     */
    fun findById(paymentId: String): Payment? {
        return payments[paymentId]
    }
    
    /**
     * 모든 결제를 조회합니다.
     * 
     * @return 전체 결제 목록
     */
    fun findAll(): List<Payment> {
        return payments.values.toList()
    }
    
    /**
     * 예약 ID로 관련된 모든 결제를 조회합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 해당 예약의 결제 목록
     */
    fun findByReservationId(reservationId: String): List<Payment> {
        return payments.values.filter { it.reservationId == reservationId }
    }
    
    /**
     * 결제 상태로 결제를 조회합니다.
     * 
     * @param status 결제 상태 (SUCCESS, FAILED, CANCELLED 등)
     * @return 해당 상태의 결제 목록
     */
    fun findByStatus(status: PaymentStatus): List<Payment> {
        return payments.values.filter { it.status == status }
    }
    
    /**
     * 특정 결제가 존재하는지 확인합니다.
     * 
     * @param paymentId 결제 식별자
     * @return 존재 여부
     */
    fun existsById(paymentId: String): Boolean {
        return payments.containsKey(paymentId)
    }
    
    /**
     * 특정 결제를 삭제합니다.
     * 
     * @param paymentId 삭제할 결제 식별자
     */
    fun deleteById(paymentId: String) {
        payments.remove(paymentId)
    }
    
    /**
     * 저장된 결제 총 개수를 반환합니다.
     * 
     * @return 전체 결제 수
     */
    fun count(): Long {
        return payments.size.toLong()
    }
    
    /**
     * 모든 결제를 삭제합니다.
     */
    fun deleteAll() {
        payments.clear()
    }
}