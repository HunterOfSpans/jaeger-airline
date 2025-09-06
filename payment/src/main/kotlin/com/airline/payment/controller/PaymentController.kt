package com.airline.payment.controller

import com.airline.payment.dto.PaymentRequest
import com.airline.payment.dto.PaymentResponse
import com.airline.payment.service.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 결제 관리 REST API 컨트롤러
 * 
 * 결제 처리, 조회, 취소 등의 HTTP API를 제공합니다.
 * OpenTelemetry의 자동 계측을 통해 분산 추적을 지원하며,
 * RESTful API 설계 원칙을 따릅니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/payments")
class PaymentController(
    private val paymentService: PaymentService
) {
    
    /**
     * 결제를 처리합니다.
     * 
     * @param request 결제 요청 정보
     * @return 결제 처리 결과
     */
    @PostMapping
    fun processPayment(@RequestBody request: PaymentRequest): ResponseEntity<PaymentResponse> {
        val response = paymentService.processPayment(request)
        return ResponseEntity.ok(response)
    }
    
    /**
     * 결제 ID로 특정 결제 정보를 조회합니다.
     * 
     * @param paymentId 결제 식별자
     * @return 결제 정보, 존재하지 않으면 404 Not Found
     */
    @GetMapping("/{paymentId}")
    fun getPaymentById(@PathVariable paymentId: String): ResponseEntity<PaymentResponse> {
        val payment = paymentService.getPaymentById(paymentId)
        return if (payment != null) {
            ResponseEntity.ok(payment)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    /**
     * 결제를 취소합니다.
     * 
     * @param paymentId 취소할 결제 식별자
     * @return 취소된 결제 정보, 실패 시 400 Bad Request
     */
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