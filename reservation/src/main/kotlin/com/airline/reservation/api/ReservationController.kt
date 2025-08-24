package com.airline.reservation.api

import com.airline.reservation.dto.ReservationRequest
import com.airline.reservation.dto.ReservationResponse
import com.airline.reservation.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {
    
    @PostMapping
    fun createReservation(@RequestBody request: ReservationRequest): ResponseEntity<ReservationResponse> {
        val response = reservationService.createReservation(request)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/{reservationId}")
    fun getReservationById(@PathVariable reservationId: String): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.getReservationById(reservationId)
        return if (reservation != null) {
            ResponseEntity.ok(reservation)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{reservationId}/cancel")
    fun cancelReservation(@PathVariable reservationId: String): ResponseEntity<ReservationResponse> {
        val cancelledReservation = reservationService.cancelReservation(reservationId)
        return if (cancelledReservation != null) {
            ResponseEntity.ok(cancelledReservation)
        } else {
            ResponseEntity.badRequest().build()
        }
    }
    
    // 기존 호환성을 위한 간단한 엔드포인트 유지
    @PostMapping("/simple")
    fun createSimpleReservation(): ResponseEntity<Void> {
        reservationService.reserve()
        return ResponseEntity.ok().build()
    }
}