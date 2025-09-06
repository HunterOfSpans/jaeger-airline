package com.airline.flight.exception;

/**
 * Kafka 메시지 처리 중 발생하는 예외
 * 
 * @author Claude Code
 * @since 1.0
 */
public class KafkaMessageProcessingException extends RuntimeException {
    
    public KafkaMessageProcessingException(String message) {
        super(message);
    }
    
    public KafkaMessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}