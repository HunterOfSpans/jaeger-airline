package com.airline.payment.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * 결제 서비스 글로벌 예외 핸들러
 * 
 * 모든 예외를 일관된 형태로 처리하여 클라이언트에게 명확한 에러 응답을 제공합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 결제를 찾을 수 없는 경우 처리
     */
    @ExceptionHandler(PaymentNotFoundException::class)
    fun handlePaymentNotFoundException(
        ex: PaymentNotFoundException, 
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.warn("Payment not found: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "PAYMENT_NOT_FOUND",
            ex.message ?: "Payment not found",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.NOT_FOUND)
    }

    /**
     * 결제 처리 실패 처리
     */
    @ExceptionHandler(PaymentProcessingException::class)
    fun handlePaymentProcessingException(
        ex: PaymentProcessingException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.warn("Payment processing failed: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.PAYMENT_REQUIRED,
            "PAYMENT_PROCESSING_FAILED",
            ex.message ?: "Payment processing failed",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.PAYMENT_REQUIRED)
    }

    /**
     * 잘못된 결제 요청 처리
     */
    @ExceptionHandler(InvalidPaymentRequestException::class)
    fun handleInvalidPaymentRequestException(
        ex: InvalidPaymentRequestException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.warn("Invalid payment request: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_PAYMENT_REQUEST",
            ex.message ?: "Invalid payment request",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    /**
     * 이미 취소된 결제 처리
     */
    @ExceptionHandler(PaymentAlreadyCancelledException::class)
    fun handlePaymentAlreadyCancelledException(
        ex: PaymentAlreadyCancelledException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.warn("Payment already cancelled: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.CONFLICT,
            "PAYMENT_ALREADY_CANCELLED",
            ex.message ?: "Payment already cancelled",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.CONFLICT)
    }

    /**
     * 일반적인 RuntimeException 처리
     */
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.error("Unexpected runtime error: {}", ex.message, ex)
        
        val body = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * 모든 예외의 최종 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.error("Unexpected error: {}", ex.message, ex)
        
        val body = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * 에러 응답 표준 형식 생성
     */
    private fun createErrorResponse(
        status: HttpStatus, 
        errorCode: String, 
        message: String, 
        path: String
    ): Map<String, Any> {
        
        return mapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to status.value(),
            "error" to status.reasonPhrase,
            "errorCode" to errorCode,
            "message" to message,
            "path" to path.replace("uri=", "")
        )
    }
}