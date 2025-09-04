package com.airline.ticket.aspect

import com.airline.ticket.annotation.KafkaOtelTrace
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Aspect
@Component
class KafkaTracingAspect(private val openTelemetry: OpenTelemetry) {
    private val logger = LoggerFactory.getLogger(KafkaTracingAspect::class.java)

    @Around("@annotation(kafkaOtelTrace)")
    fun traceKafkaListener(pjp: ProceedingJoinPoint, kafkaOtelTrace: KafkaOtelTrace): Any? {
        val record = pjp.args.filterIsInstance<ConsumerRecord<*, *>>().firstOrNull()
        if (record == null) {
            logger.warn("@KafkaOtelTrace used on method without ConsumerRecord parameter: {}", pjp.signature.name)
            return pjp.proceed()
        }

        logger.debug("Applying @KafkaOtelTrace to method: {} with topic: {}", pjp.signature.name, record.topic())

        // 메시지 헤더에서 Trace Context 추출
        val getter = object : TextMapGetter<ConsumerRecord<*, *>> {
            override fun keys(carrier: ConsumerRecord<*, *>): Iterable<String> =
                carrier.headers().map { it.key() }

            override fun get(carrier: ConsumerRecord<*, *>?, key: String): String? =
                carrier?.headers()?.lastHeader(key)?.value()?.toString(Charsets.UTF_8)
        }

        val propagators = openTelemetry.propagators
        val parentContext = propagators.textMapPropagator
            .extract(Context.current(), record, getter)

        // Span 이름 결정
        val spanName = if (kafkaOtelTrace.spanName.isNotBlank()) {
            kafkaOtelTrace.spanName
        } else {
            "KafkaConsumer.${pjp.signature.name}"
        }

        // 새 Span 시작
        val tracer = openTelemetry.getTracer("kafka-otel-trace")
        val spanBuilder = tracer.spanBuilder(spanName)
            .setSpanKind(SpanKind.CONSUMER)
            .setParent(parentContext)
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination", record.topic())
            .setAttribute("messaging.destination_kind", "topic")
            .setAttribute("messaging.kafka.partition", record.partition().toLong())
            .setAttribute("messaging.kafka.offset", record.offset())

        // 커스텀 속성 추가
        kafkaOtelTrace.attributes.forEach { attr ->
            val parts = attr.split("=", limit = 2)
            if (parts.size == 2) {
                spanBuilder.setAttribute(parts[0], parts[1])
            }
        }

        // 메시지 내용 기록 (옵션)
        if (kafkaOtelTrace.recordMessageContent && record.value() != null) {
            spanBuilder.setAttribute("messaging.message_payload", record.value().toString())
        }

        val span = spanBuilder.startSpan()

        return try {
            span.makeCurrent().use {
                logger.debug("Executing Kafka listener with trace context: {}", span.spanContext.traceId)
                pjp.proceed()
            }
        } catch (e: Exception) {
            if (kafkaOtelTrace.recordException) {
                span.recordException(e)
                span.setStatus(StatusCode.ERROR, e.message ?: "Kafka listener execution failed")
            }
            logger.error("Error in Kafka listener with tracing", e)
            throw e
        } finally {
            span.end()
            logger.debug("Completed Kafka listener trace for: {}", spanName)
        }
    }

}