package com.airline.payment.aspect

import com.airline.payment.annotation.KafkaOtelTrace
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
        // 1. MessageHeaders 우선 탐지 (더 깔끔한 방식)
        val messageHeaders = pjp.args.filterIsInstance<org.springframework.messaging.MessageHeaders>().firstOrNull()
        if (messageHeaders != null) {
            return traceWithMessageHeaders(pjp, kafkaOtelTrace, messageHeaders)
        }

        // 2. ConsumerRecord 탐지 (기존 방식 호환성 유지)
        val record = pjp.args.filterIsInstance<ConsumerRecord<*, *>>().firstOrNull()
        if (record != null) {
            return traceWithConsumerRecord(pjp, kafkaOtelTrace, record)
        }

        logger.warn("@KafkaOtelTrace used on method without MessageHeaders or ConsumerRecord parameter: {}", pjp.signature.name)
        return pjp.proceed()
    }

    private fun traceWithMessageHeaders(
        pjp: ProceedingJoinPoint, 
        kafkaOtelTrace: KafkaOtelTrace, 
        messageHeaders: org.springframework.messaging.MessageHeaders
    ): Any? {
        logger.debug("Applying @KafkaOtelTrace to method: {} with MessageHeaders", pjp.signature.name)

        // MessageHeaders에서 Trace Context 추출
        val getter = object : TextMapGetter<org.springframework.messaging.MessageHeaders> {
            override fun keys(carrier: org.springframework.messaging.MessageHeaders): Iterable<String> = carrier.keys

            override fun get(carrier: org.springframework.messaging.MessageHeaders?, key: String): String? =
                carrier?.get(key)?.toString()
        }

        val propagators = openTelemetry.propagators
        val parentContext = propagators.textMapPropagator.extract(Context.current(), messageHeaders, getter)

        // MessageHeaders에서 Kafka 메타데이터 추출
        val topic = messageHeaders[org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC]?.toString() ?: "unknown"
        val partition = messageHeaders[org.springframework.kafka.support.KafkaHeaders.RECEIVED_PARTITION] as? Int ?: -1
        val offset = messageHeaders[org.springframework.kafka.support.KafkaHeaders.OFFSET] as? Long ?: -1L

        return createAndExecuteSpan(pjp, kafkaOtelTrace, parentContext, topic, partition, offset, null)
    }

    private fun traceWithConsumerRecord(
        pjp: ProceedingJoinPoint, 
        kafkaOtelTrace: KafkaOtelTrace, 
        record: ConsumerRecord<*, *>
    ): Any? {
        logger.debug("Applying @KafkaOtelTrace to method: {} with ConsumerRecord topic: {}", pjp.signature.name, record.topic())

        // ConsumerRecord 헤더에서 Trace Context 추출
        val getter = object : TextMapGetter<ConsumerRecord<*, *>> {
            override fun keys(carrier: ConsumerRecord<*, *>): Iterable<String> =
                carrier.headers().map { it.key() }

            override fun get(carrier: ConsumerRecord<*, *>?, key: String): String? =
                carrier?.headers()?.lastHeader(key)?.value()?.toString(Charsets.UTF_8)
        }

        val propagators = openTelemetry.propagators
        val parentContext = propagators.textMapPropagator.extract(Context.current(), record, getter)

        return createAndExecuteSpan(pjp, kafkaOtelTrace, parentContext, record.topic(), record.partition(), record.offset(), record.value())
    }

    private fun createAndExecuteSpan(
        pjp: ProceedingJoinPoint,
        kafkaOtelTrace: KafkaOtelTrace,
        parentContext: Context,
        topic: String,
        partition: Int,
        offset: Long,
        messageValue: Any?
    ): Any? {

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
            .setAttribute("messaging.destination", topic)
            .setAttribute("messaging.destination_kind", "topic")
            .setAttribute("messaging.kafka.partition", partition.toLong())
            .setAttribute("messaging.kafka.offset", offset)

        // 커스텀 속성 추가
        kafkaOtelTrace.attributes.forEach { attr ->
            val parts = attr.split("=", limit = 2)
            if (parts.size == 2) {
                spanBuilder.setAttribute(parts[0], parts[1])
            }
        }

        // 메시지 내용 기록 (옵션)
        if (kafkaOtelTrace.recordMessageContent && messageValue != null) {
            spanBuilder.setAttribute("messaging.message_payload", messageValue.toString())
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