package com.airline.flight.exception;

/**
 * 잘못된 요청일 때 발생하는 예외
 * 
 * @author Claude Code
 * @since 1.0
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}