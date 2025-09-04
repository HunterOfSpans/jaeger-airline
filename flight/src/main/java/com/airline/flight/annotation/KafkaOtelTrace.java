package com.airline.flight.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kafka 메시지 처리에 OpenTelemetry 분산 추적을 적용하는 커스텀 어노테이션
 * 
 * @KafkaListener와 함께 사용하여 명시적으로 분산 추적을 활성화합니다.
 * 
 * 사용 예시:
 * <pre>
 * @KafkaListener(topics = {"reservation.requested"})
 * @KafkaOtelTrace(spanName = "process-reservation-request")
 * public void handleReservationRequest(String message) { ... }
 * </pre>
 * 
 * @param spanName 커스텀 span 이름 (기본값: 메서드명 기반 자동 생성)
 * @param attributes 추가할 span 속성들 (key=value 형태의 배열)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaOtelTrace {
    
    /**
     * 커스텀 span 이름
     * 빈 문자열인 경우 "KafkaConsumer.{메서드명}" 형태로 자동 생성
     */
    String spanName() default "";
    
    /**
     * span에 추가할 커스텀 속성들
     * 형태: {"key1=value1", "key2=value2"}
     */
    String[] attributes() default {};
    
    /**
     * 에러 발생 시 span에 기록할지 여부
     */
    boolean recordException() default true;
    
    /**
     * 메시지 내용을 span 속성으로 기록할지 여부
     * (보안상 민감한 정보가 있을 경우 false로 설정)
     */
    boolean recordMessageContent() default false;
}