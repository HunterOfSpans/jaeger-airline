package com.airline.reservation.client

import com.airline.reservation.dto.external.PaymentRequest
import com.airline.reservation.dto.external.PaymentResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(name = "payment-service", url = "\${services.payment.url:http://payment:8082}")
interface PaymentClient {
    
    @PostMapping("/v1/payments")
    fun processPayment(@RequestBody request: PaymentRequest): PaymentResponse
    
    @GetMapping("/v1/payments/{paymentId}")
    fun getPaymentById(@PathVariable paymentId: String): PaymentResponse?
    
    @PostMapping("/v1/payments/{paymentId}/cancel")
    fun cancelPayment(@PathVariable paymentId: String): PaymentResponse?
}