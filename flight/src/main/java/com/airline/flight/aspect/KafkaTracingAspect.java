package com.airline.flight.aspect;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class KafkaTracingAspect {
    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    public KafkaTracingAspect(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("kafka-listener");
    }

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object traceKafkaListener(ProceedingJoinPoint pjp) throws Throwable {

        // 1. ConsumerRecord 추출
        ConsumerRecord<?, ?> record = Arrays.stream(pjp.getArgs())
                .filter(ConsumerRecord.class::isInstance)
                .map(ConsumerRecord.class::cast)
                .findFirst()
                .orElse(null);

        if (record == null) {
            return pjp.proceed();
        }

        // 2. 메시지 헤더에서 Trace Context 추출
        TextMapGetter<ConsumerRecord<?, ?>> getter = new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(ConsumerRecord<?, ?> carrier) {
                List<String> keyList = new ArrayList<>();
                carrier.headers().forEach(header -> keyList.add(header.key()));
                return keyList;
            }

            @Override
            public String get(ConsumerRecord<?, ?> carrier, String key) {
                if (carrier == null) return null;
                Header header = carrier.headers().lastHeader(key);
                if (header == null) return null;
                return new String(header.value(), StandardCharsets.UTF_8);
            }
        };

        Context parentContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record, getter);

        // 3. 새로운 Span 생성
        final String spanName = String.format("KafkaListener.%s", pjp.getSignature().getName());
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(parentContext)
                .startSpan();

        // 4. Span 안에서 원래 메서드 실행
        try (Scope scope = span.makeCurrent()) {
            return pjp.proceed();
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage() != null ? e.getMessage() : "error");
            throw e;
        } finally {
            span.end();
        }
    }
}
