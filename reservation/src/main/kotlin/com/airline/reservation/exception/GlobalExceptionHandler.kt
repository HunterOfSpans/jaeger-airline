package com.airline.reservation.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * 예약 서비스 글로벌 예외 핸들러
 * 
 * @author Claude Code
 * @since 1.0
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ReservationNotFoundException::class)
    fun handleReservationNotFoundException(
        ex: ReservationNotFoundException, 
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.warn("Reservation not found: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "RESERVATION_NOT_FOUND",
            ex.message ?: "Reservation not found",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(ReservationProcessingException::class)
    fun handleReservationProcessingException(
        ex: ReservationProcessingException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.error("Reservation processing failed: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "RESERVATION_PROCESSING_FAILED",
            ex.message ?: "Reservation processing failed",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(InvalidReservationRequestException::class)
    fun handleInvalidReservationRequestException(
        ex: InvalidReservationRequestException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.warn("Invalid reservation request: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_RESERVATION_REQUEST",
            ex.message ?: "Invalid reservation request",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(FlightServiceException::class)
    fun handleFlightServiceException(
        ex: FlightServiceException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.error("Flight service error: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.BAD_GATEWAY,
            "FLIGHT_SERVICE_ERROR",
            ex.message ?: "Flight service error",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.BAD_GATEWAY)
    }

    @ExceptionHandler(PaymentServiceException::class)
    fun handlePaymentServiceException(
        ex: PaymentServiceException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.error("Payment service error: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.PAYMENT_REQUIRED,
            "PAYMENT_SERVICE_ERROR",
            ex.message ?: "Payment service error",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.PAYMENT_REQUIRED)
    }

    @ExceptionHandler(TicketServiceException::class)
    fun handleTicketServiceException(
        ex: TicketServiceException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        
        logger.error("Ticket service error: {}", ex.message)
        
        val body = createErrorResponse(
            HttpStatus.BAD_GATEWAY,
            "TICKET_SERVICE_ERROR",
            ex.message ?: "Ticket service error",
            request.getDescription(false)
        )
        
        return ResponseEntity(body, HttpStatus.BAD_GATEWAY)
    }

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