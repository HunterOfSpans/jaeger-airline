package com.airline.reservation.repository

import com.airline.reservation.entity.Reservation
import com.airline.reservation.dto.ReservationStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * 예약 데이터 접근 계층
 * 
 * 예약 정보의 CRUD 작업과 비즈니스 로직에 특화된 조회 기능을 제공합니다.
 * ConcurrentHashMap을 사용한 인메모리 저장소로 구현되어 있으며,
 * 실제 프로덕션에서는 JPA Repository로 교체 가능한 구조입니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Repository
class ReservationRepository {
    
    private val reservations = ConcurrentHashMap<String, Reservation>()
    
    /**
     * 예약 정보를 저장하거나 업데이트합니다.
     * 
     * @param reservation 저장할 예약 엔티티
     * @return 저장된 예약 엔티티
     */
    fun save(reservation: Reservation): Reservation {
        reservations[reservation.reservationId] = reservation
        return reservation
    }
    
    /**
     * 예약 ID로 특정 예약을 조회합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 예약 엔티티, 존재하지 않으면 null
     */
    fun findById(reservationId: String): Reservation? {
        return reservations[reservationId]
    }
    
    /**
     * 모든 예약을 조회합니다.
     * 
     * @return 전체 예약 목록
     */
    fun findAll(): List<Reservation> {
        return reservations.values.toList()
    }
    
    /**
     * 항공편 ID로 관련된 모든 예약을 조회합니다.
     * 
     * @param flightId 항공편 식별자
     * @return 해당 항공편의 예약 목록
     */
    fun findByFlightId(flightId: String): List<Reservation> {
        return reservations.values.filter { it.flightId == flightId }
    }
    
    /**
     * 승객 이메일로 관련된 모든 예약을 조회합니다.
     * 
     * @param email 승객 이메일 주소
     * @return 해당 승객의 예약 목록
     */
    fun findByPassengerEmail(email: String): List<Reservation> {
        return reservations.values.filter { it.passengerEmail == email }
    }
    
    /**
     * 예약 상태로 예약을 조회합니다.
     * 
     * @param status 예약 상태 (PENDING, CONFIRMED, CANCELLED 등)
     * @return 해당 상태의 예약 목록
     */
    fun findByStatus(status: ReservationStatus): List<Reservation> {
        return reservations.values.filter { it.status == status }
    }
    
    /**
     * 결제 ID로 관련된 모든 예약을 조회합니다.
     * 
     * @param paymentId 결제 식별자
     * @return 해당 결제의 예약 목록
     */
    fun findByPaymentId(paymentId: String): List<Reservation> {
        return reservations.values.filter { it.paymentId == paymentId }
    }
    
    /**
     * 특정 예약이 존재하는지 확인합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 존재 여부
     */
    fun existsById(reservationId: String): Boolean {
        return reservations.containsKey(reservationId)
    }
    
    /**
     * 특정 예약을 삭제합니다.
     * 
     * @param reservationId 삭제할 예약 식별자
     */
    fun deleteById(reservationId: String) {
        reservations.remove(reservationId)
    }
    
    /**
     * 저장된 예약 총 개수를 반환합니다.
     * 
     * @return 전체 예약 수
     */
    fun count(): Long {
        return reservations.size.toLong()
    }
    
    /**
     * 모든 예약을 삭제합니다.
     */
    fun deleteAll() {
        reservations.clear()
    }
}