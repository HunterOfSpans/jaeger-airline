package com.airline.reservation.client

import com.airline.reservation.dto.external.FlightDto
import com.airline.reservation.dto.external.AvailabilityRequest
import com.airline.reservation.dto.external.AvailabilityResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(name = "flight-service", url = "\${services.flight.url:http://flight:8080}")
interface FlightClient {
    
    @GetMapping("/v1/flights/{flightId}")
    fun getFlightById(@PathVariable flightId: String): FlightDto?
    
    @PostMapping("/v1/flights/{flightId}/availability")
    fun checkAvailability(
        @PathVariable flightId: String,
        @RequestBody request: AvailabilityRequest
    ): AvailabilityResponse
    
    @PostMapping("/v1/flights/{flightId}/reserve")
    fun reserveSeats(
        @PathVariable flightId: String,
        @RequestBody request: AvailabilityRequest
    ): Void?
    
    @PostMapping("/v1/flights/{flightId}/release")
    fun releaseSeats(
        @PathVariable flightId: String,
        @RequestBody request: AvailabilityRequest
    ): Void?
}