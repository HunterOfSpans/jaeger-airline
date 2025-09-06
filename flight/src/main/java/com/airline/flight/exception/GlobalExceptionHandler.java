package com.airline.flight.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 항공편 서비스 글로벌 예외 핸들러
 * 
 * 모든 예외를 일관된 형태로 처리하여 클라이언트에게 명확한 에러 응답을 제공합니다.
 * 
 * @author Claude Code
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 항공편을 찾을 수 없는 경우 처리
     */
    @ExceptionHandler(FlightNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFlightNotFoundException(
            FlightNotFoundException ex, WebRequest request) {
        
        log.warn("Flight not found: {}", ex.getMessage());
        
        Map<String, Object> body = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "FLIGHT_NOT_FOUND", 
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * 좌석이 부족한 경우 처리
     */
    @ExceptionHandler(InsufficientSeatsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientSeatsException(
            InsufficientSeatsException ex, WebRequest request) {
        
        log.warn("Insufficient seats: {}", ex.getMessage());
        
        Map<String, Object> body = createErrorResponse(
            HttpStatus.CONFLICT,
            "INSUFFICIENT_SEATS",
            ex.getMessage(),
            request.getDescription(false)
        );
        
        body.put("flightId", ex.getFlightId());
        body.put("requestedSeats", ex.getRequestedSeats());
        body.put("availableSeats", ex.getAvailableSeats());
        
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * 잘못된 요청 처리
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequestException(
            InvalidRequestException ex, WebRequest request) {
        
        log.warn("Invalid request: {}", ex.getMessage());
        
        Map<String, Object> body = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST",
            ex.getMessage(),
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 일반적인 RuntimeException 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        log.error("Unexpected runtime error: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 모든 예외의 최종 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getDescription(false)
        );
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 에러 응답 표준 형식 생성
     */
    private Map<String, Object> createErrorResponse(HttpStatus status, String errorCode, 
            String message, String path) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("path", path.replace("uri=", ""));
        
        return body;
    }
}