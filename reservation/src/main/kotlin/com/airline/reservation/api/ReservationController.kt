package com.airline.reservation.api

import com.airline.reservation.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {
    @PostMapping
    fun createReservation() : ResponseEntity<Void> {
        
        reservationService.reserve()
        return ResponseEntity.ok().build()
    }
}