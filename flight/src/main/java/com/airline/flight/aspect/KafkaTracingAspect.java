package com.airline.flight.aspect;

import com.airline.flight.annotation.KafkaOtelTrace;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.stream.StreamSupport;

@Aspect
@Component
public class KafkaTracingAspect {
    private final Logger logger = LoggerFactory.getLogger(KafkaTracingAspect.class);
    private final OpenTelemetry openTelemetry;

    public KafkaTracingAspect(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Around("@annotation(kafkaOtelTrace)")
    public Object traceKafkaListener(ProceedingJoinPoint pjp, KafkaOtelTrace kafkaOtelTrace) throws Throwable {
        // 1. MessageHeaders 우선 탐지 (더 깔끔한 방식)
        MessageHeaders messageHeaders = null;
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof MessageHeaders) {
                messageHeaders = (MessageHeaders) arg;
                break;
            }
        }
        
        if (messageHeaders != null) {
            return traceWithMessageHeaders(pjp, kafkaOtelTrace, messageHeaders);
        }

        // 2. ConsumerRecord 탐지 (기존 방식 호환성 유지)
        ConsumerRecord<?, ?> record = null;
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof ConsumerRecord) {
                record = (ConsumerRecord<?, ?>) arg;
                break;
            }
        }
        
        if (record != null) {
            return traceWithConsumerRecord(pjp, kafkaOtelTrace, record);
        }

        logger.warn("@KafkaOtelTrace used on method without MessageHeaders or ConsumerRecord parameter: {}", pjp.getSignature().getName());
        return pjp.proceed();
    }

    private Object traceWithMessageHeaders(ProceedingJoinPoint pjp, KafkaOtelTrace kafkaOtelTrace, MessageHeaders messageHeaders) throws Throwable {
        logger.debug("Applying @KafkaOtelTrace to method: {} with MessageHeaders", pjp.getSignature().getName());

        // MessageHeaders에서 Trace Context 추출
        TextMapGetter<MessageHeaders> getter = new TextMapGetter<MessageHeaders>() {
            @Override
            public Iterable<String> keys(@Nonnull MessageHeaders carrier) {
                return carrier.keySet();
            }

            @Nullable
            @Override
            public String get(@Nullable MessageHeaders carrier, @Nonnull String key) {
                if (carrier == null) return null;
                Object value = carrier.get(key);
                return value != null ? value.toString() : null;
            }
        };

        Context parentContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), messageHeaders, getter);

        // MessageHeaders에서 Kafka 메타데이터 추출
        String topic = messageHeaders.get(KafkaHeaders.RECEIVED_TOPIC) != null ? 
                messageHeaders.get(KafkaHeaders.RECEIVED_TOPIC).toString() : "unknown";
        Integer partition = (Integer) messageHeaders.get(KafkaHeaders.RECEIVED_PARTITION);
        Long offset = (Long) messageHeaders.get(KafkaHeaders.OFFSET);

        return createAndExecuteSpan(pjp, kafkaOtelTrace, parentContext, topic, 
                partition != null ? partition : -1, 
                offset != null ? offset : -1L, 
                null);
    }

    private Object traceWithConsumerRecord(ProceedingJoinPoint pjp, KafkaOtelTrace kafkaOtelTrace, ConsumerRecord<?, ?> record) throws Throwable {
        logger.debug("Applying @KafkaOtelTrace to method: {} with ConsumerRecord topic: {}", pjp.getSignature().getName(), record.topic());

        // ConsumerRecord 헤더에서 Trace Context 추출
        TextMapGetter<ConsumerRecord<?, ?>> getter = new TextMapGetter<ConsumerRecord<?, ?>>() {
            @Override
            public Iterable<String> keys(@Nonnull ConsumerRecord<?, ?> carrier) {
                return StreamSupport.stream(carrier.headers().spliterator(), false)
                        .map(header -> header.key())::iterator;
            }

            @Nullable
            @Override
            public String get(@Nullable ConsumerRecord<?, ?> carrier, @Nonnull String key) {
                if (carrier == null) return null;
                var header = carrier.headers().lastHeader(key);
                return header != null ? new String(header.value()) : null;
            }
        };

        Context parentContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), record, getter);

        return createAndExecuteSpan(pjp, kafkaOtelTrace, parentContext, record.topic(), 
                record.partition(), record.offset(), 
                record.value());
    }

    private Object createAndExecuteSpan(ProceedingJoinPoint pjp, KafkaOtelTrace kafkaOtelTrace, 
                                      Context parentContext, String topic, int partition, long offset, 
                                      Object messageValue) throws Throwable {
        // Span 이름 결정
        String spanName = kafkaOtelTrace.spanName().isEmpty() ? 
                "KafkaConsumer." + pjp.getSignature().getName() : 
                kafkaOtelTrace.spanName();

        // 새 Span 시작
        Tracer tracer = openTelemetry.getTracer("kafka-otel-trace");
        SpanBuilder spanBuilder = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(parentContext)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", topic)
                .setAttribute("messaging.destination_kind", "topic")
                .setAttribute("messaging.kafka.partition", (long) partition)
                .setAttribute("messaging.kafka.offset", offset);

        // 커스텀 속성 추가
        for (String attr : kafkaOtelTrace.attributes()) {
            String[] parts = attr.split("=", 2);
            if (parts.length == 2) {
                spanBuilder.setAttribute(parts[0], parts[1]);
            }
        }

        // 메시지 내용 기록 (옵션)
        if (kafkaOtelTrace.recordMessageContent() && messageValue != null) {
            spanBuilder.setAttribute("messaging.message_payload", messageValue.toString());
        }

        var span = spanBuilder.startSpan();

        try (var scope = span.makeCurrent()) {
            logger.debug("Executing Kafka listener with trace context: {}", span.getSpanContext().getTraceId());
            return pjp.proceed();
        } catch (Exception e) {
            if (kafkaOtelTrace.recordException()) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage() != null ? e.getMessage() : "Kafka listener execution failed");
            }
            logger.error("Error in Kafka listener with tracing", e);
            throw e;
        } finally {
            span.end();
            logger.debug("Completed Kafka listener trace for: {}", spanName);
        }
    }
}