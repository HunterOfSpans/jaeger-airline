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
        ConsumerRecord<?, ?> record = null;
        
        // ConsumerRecord 찾기
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof ConsumerRecord) {
                record = (ConsumerRecord<?, ?>) arg;
                break;
            }
        }
        
        if (record == null) {
            logger.warn("@KafkaOtelTrace used on method without ConsumerRecord parameter: {}", pjp.getSignature().getName());
            return pjp.proceed();
        }

        logger.debug("Applying @KafkaOtelTrace to method: {} with topic: {}", pjp.getSignature().getName(), record.topic());

        // 메시지 헤더에서 Trace Context 추출
        TextMapGetter<ConsumerRecord<?, ?>> getter = new TextMapGetter<ConsumerRecord<?, ?>>() {
            @Override
            public Iterable<String> keys(@Nonnull ConsumerRecord<?, ?> carrier) {
                return () -> StreamSupport.stream(carrier.headers().spliterator(), false)
                    .map(header -> header.key())
                    .iterator();
            }

            @Override
            @Nullable
            public String get(@Nullable ConsumerRecord<?, ?> carrier, @Nonnull String key) {
                if (carrier == null) return null;
                var header = carrier.headers().lastHeader(key);
                return header != null ? new String(header.value()) : null;
            }
        };

        Context parentContext = openTelemetry.getPropagators()
            .getTextMapPropagator()
            .extract(Context.current(), record, getter);

        // Span 이름 결정
        String spanName = kafkaOtelTrace.spanName().isEmpty() 
            ? "KafkaConsumer." + pjp.getSignature().getName()
            : kafkaOtelTrace.spanName();

        // 새 Span 시작
        Tracer tracer = openTelemetry.getTracer("kafka-otel-trace");
        SpanBuilder spanBuilder = tracer.spanBuilder(spanName)
            .setSpanKind(SpanKind.CONSUMER)
            .setParent(parentContext)
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination", record.topic())
            .setAttribute("messaging.destination_kind", "topic")
            .setAttribute("messaging.kafka.partition", (long) record.partition())
            .setAttribute("messaging.kafka.offset", record.offset());

        // 커스텀 속성 추가
        for (String attr : kafkaOtelTrace.attributes()) {
            String[] parts = attr.split("=", 2);
            if (parts.length == 2) {
                spanBuilder.setAttribute(parts[0], parts[1]);
            }
        }

        // 메시지 내용 기록 (옵션)
        if (kafkaOtelTrace.recordMessageContent() && record.value() != null) {
            spanBuilder.setAttribute("messaging.message_payload", record.value().toString());
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