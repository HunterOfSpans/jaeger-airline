package com.airline.reservation.api

import arrow.core.Either
import com.airline.reservation.common.DomainError
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
    suspend fun createReservation(@RequestBody request: ReservationRequest): ResponseEntity<ReservationResponse> {
        return when (val result = reservationService.createReservation(request)) {
            is Either.Left -> ResponseEntity.badRequest().build()
            is Either.Right -> ResponseEntity.ok(result.value)
        }
    }
    
    /**
     * 예약 ID로 특정 예약 정보를 조회합니다.
     * 
     * @param reservationId 예약 식별자
     * @return 예약 정보, 존재하지 않으면 404 Not Found
     */
    @GetMapping("/{reservationId}")
    suspend fun getReservationById(@PathVariable reservationId: String): ResponseEntity<ReservationResponse> {
        return when (val result = reservationService.getReservationById(reservationId)) {
            is Either.Left -> ResponseEntity.notFound().build()
            is Either.Right -> ResponseEntity.ok(result.value)
        }
    }
    
    /**
     * 예약을 취소합니다.
     *
     * @param reservationId 취소할 예약 식별자
     * @return 취소된 예약 정보, 실패 시 400 Bad Request
     */
    @PostMapping("/{reservationId}/cancel")
    suspend fun cancelReservation(@PathVariable reservationId: String): ResponseEntity<ReservationResponse> {
        return when (val result = reservationService.cancelReservation(reservationId)) {
            is Either.Left -> ResponseEntity.badRequest().build()
            is Either.Right -> ResponseEntity.ok(result.value)
        }
    }
}