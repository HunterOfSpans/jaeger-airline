package com.airline.payment.controller

import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.service.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/payments")
class PaymentController(
    private val paymentService: PaymentService
) {
    
    @PostMapping
    fun processPayment(@RequestBody request: PaymentRequest): ResponseEntity<PaymentResponse> {
        val response = paymentService.processPayment(request)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/{paymentId}")
    fun getPaymentById(@PathVariable paymentId: String): ResponseEntity<PaymentResponse> {
        val payment = paymentService.getPaymentById(paymentId)
        return if (payment != null) {
            ResponseEntity.ok(payment)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{paymentId}/cancel")
    fun cancelPayment(@PathVariable paymentId: String): ResponseEntity<PaymentResponse> {
        val cancelledPayment = paymentService.cancelPayment(paymentId)
        return if (cancelledPayment != null) {
            ResponseEntity.ok(cancelledPayment)
        } else {
            ResponseEntity.badRequest().build()
        }
    }
}