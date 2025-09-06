package com.airline.reservation.api

import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 예약 관리 REST API 컨트롤러
 * 
 * 예약 생성, 조회, 취소 등의 HTTP API를 제공합니다.
 * OpenTelemetry의 자동 계측을 통해 분산 추적을 지원하며,
 * RESTful API 설계 원칙을 따릅니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@RestController
@RequestMapping("v1/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {
    
    /**
     * 예약을 생성합니다.
     * 
     * @param request 예약 생성 요청 정보
     * @return 생성된 예약 정보
     */
    @PostMapping
    fun createReservation(@RequestBody request: ReservationRequest): ResponseEntity<ReservationResponse> {
        val response = reservationService.createReservation(request)
        return ResponseEntity.ok(response)
    }
    
    /**
     * 예약 ID로 특정 예약 정보를 조회합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 예약 정보, 존재하지 않으면 404 Not Found
     */
    @GetMapping("/{reservationId}")
    fun getReservationById(@PathVariable reservationId: String): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.getReservationById(reservationId)
        return if (reservation != null) {
            ResponseEntity.ok(reservation)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 예약을 취소합니다.
     * 
     * @param reservationId 취소할 예약 식별자
     * @return 취소된 예약 정보, 실패 시 400 Bad Request
     */
    @PostMapping("/{reservationId}/cancel")
    fun cancelReservation(@PathVariable reservationId: String): ResponseEntity<ReservationResponse> {
        val cancelledReservation = reservationService.cancelReservation(reservationId)
        return if (cancelledReservation != null) {
            ResponseEntity.ok(cancelledReservation)
        } else {
            ResponseEntity.badRequest().build()
        }
    }
    
    /**
     * 기존 호환성을 위한 간단한 예약 생성 엔드포인트입니다.
     * 
     * @return 200 OK 응답
     */
    @PostMapping("/simple")
    fun createSimpleReservation(): ResponseEntity<Void> {
        reservationService.reserve()
        return ResponseEntity.ok().build()
    }
}