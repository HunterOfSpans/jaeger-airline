package com.airline.tracing.aspect

import com.airline.tracing.annotation.KafkaOtelTrace
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.MessageHeaders

/**
 * Kafka Consumer 메시지 처리에 OpenTelemetry 분산 추적을 적용하는 AOP Aspect
 *
 * @KafkaOtelTrace 어노테이션이 적용된 메서드에 대해:
 * 1. Kafka 메시지 헤더에서 trace context 추출
 * 2. Parent span과 연결된 새로운 child span 생성
 * 3. 메시지 메타데이터를 span 속성으로 기록
 * 4. 예외 발생 시 에러 상태 기록
 */
@Aspect
class KafkaTracingAspect(private val openTelemetry: OpenTelemetry) {

    private val logger = LoggerFactory.getLogger(KafkaTracingAspect::class.java)

    companion object {
        private const val TRACER_NAME = "kafka-tracing"
        private const val TRACER_VERSION = "1.0.0"
    }

    @Around("@annotation(kafkaOtelTrace)")
    fun traceKafkaListener(pjp: ProceedingJoinPoint, kafkaOtelTrace: KafkaOtelTrace): Any? {
        // 1. MessageHeaders 우선 탐지 (Spring Kafka 권장 방식)
        val messageHeaders = pjp.args.filterIsInstance<MessageHeaders>().firstOrNull()
        if (messageHeaders != null) {
            return traceWithMessageHeaders(pjp, kafkaOtelTrace, messageHeaders)
        }

        // 2. ConsumerRecord 탐지 (레거시 호환성)
        val record = pjp.args.filterIsInstance<ConsumerRecord<*, *>>().firstOrNull()
        if (record != null) {
            return traceWithConsumerRecord(pjp, kafkaOtelTrace, record)
        }

        // 3. 헤더 없이 호출된 경우 경고 로그 후 진행
        logger.warn(
            "@KafkaOtelTrace used on method without MessageHeaders or ConsumerRecord parameter: {}. " +
                "Trace context will not be propagated.",
            pjp.signature.name
        )
        return pjp.proceed()
    }

    private fun traceWithMessageHeaders(
        pjp: ProceedingJoinPoint,
        kafkaOtelTrace: KafkaOtelTrace,
        messageHeaders: MessageHeaders
    ): Any? {
        logger.debug("Tracing Kafka message with MessageHeaders for method: {}", pjp.signature.name)

        // MessageHeaders에서 Trace Context 추출
        val getter = MessageHeadersTextMapGetter
        val parentContext = openTelemetry.propagators
            .textMapPropagator
            .extract(Context.current(), messageHeaders, getter)

        // Kafka 메타데이터 추출
        val topic = messageHeaders[KafkaHeaders.RECEIVED_TOPIC]?.toString() ?: "unknown"
        val partition = (messageHeaders[KafkaHeaders.RECEIVED_PARTITION] as? Int) ?: -1
        val offset = (messageHeaders[KafkaHeaders.OFFSET] as? Long) ?: -1L

        return executeWithSpan(pjp, kafkaOtelTrace, parentContext, topic, partition, offset, null)
    }

    private fun traceWithConsumerRecord(
        pjp: ProceedingJoinPoint,
        kafkaOtelTrace: KafkaOtelTrace,
        record: ConsumerRecord<*, *>
    ): Any? {
        logger.debug("Tracing Kafka message with ConsumerRecord for method: {}, topic: {}",
            pjp.signature.name, record.topic())

        // ConsumerRecord 헤더에서 Trace Context 추출
        val getter = ConsumerRecordTextMapGetter
        val parentContext = openTelemetry.propagators
            .textMapPropagator
            .extract(Context.current(), record, getter)

        return executeWithSpan(
            pjp, kafkaOtelTrace, parentContext,
            record.topic(), record.partition(), record.offset(), record.value()
        )
    }

    private fun executeWithSpan(
        pjp: ProceedingJoinPoint,
        kafkaOtelTrace: KafkaOtelTrace,
        parentContext: Context,
        topic: String,
        partition: Int,
        offset: Long,
        messageValue: Any?
    ): Any? {
        val spanName = resolveSpanName(kafkaOtelTrace, pjp)
        val tracer = openTelemetry.getTracer(TRACER_NAME, TRACER_VERSION)

        val spanBuilder = tracer.spanBuilder(spanName)
            .setSpanKind(SpanKind.CONSUMER)
            .setParent(parentContext)
            // OpenTelemetry Semantic Conventions for Messaging
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination.name", topic)
            .setAttribute("messaging.destination.kind", "topic")
            .setAttribute("messaging.kafka.partition", partition.toLong())
            .setAttribute("messaging.kafka.message.offset", offset)
            .setAttribute("messaging.operation", "receive")

        // 커스텀 속성 추가
        kafkaOtelTrace.attributes.forEach { attr ->
            val parts = attr.split("=", limit = 2)
            if (parts.size == 2) {
                spanBuilder.setAttribute(parts[0].trim(), parts[1].trim())
            }
        }

        // 메시지 내용 기록 (옵션)
        if (kafkaOtelTrace.recordMessageContent && messageValue != null) {
            val content = messageValue.toString()
            // 너무 긴 메시지는 잘라서 기록
            spanBuilder.setAttribute(
                "messaging.message.body",
                if (content.length > 1000) content.take(1000) + "..." else content
            )
        }

        val span = spanBuilder.startSpan()

        return try {
            span.makeCurrent().use {
                logger.debug("Executing Kafka consumer with traceId: {}", span.spanContext.traceId)
                pjp.proceed()
            }
        } catch (e: Exception) {
            if (kafkaOtelTrace.recordException) {
                span.recordException(e)
                span.setStatus(StatusCode.ERROR, e.message ?: "Kafka consumer execution failed")
            }
            logger.error("Error in Kafka consumer with tracing, spanName: {}", spanName, e)
            throw e
        } finally {
            span.end()
            logger.debug("Completed Kafka consumer trace: {}", spanName)
        }
    }

    private fun resolveSpanName(kafkaOtelTrace: KafkaOtelTrace, pjp: ProceedingJoinPoint): String {
        return if (kafkaOtelTrace.spanName.isNotBlank()) {
            kafkaOtelTrace.spanName
        } else {
            "kafka.consume.${pjp.signature.name}"
        }
    }

    /**
     * MessageHeaders용 TextMapGetter
     */
    private object MessageHeadersTextMapGetter : TextMapGetter<MessageHeaders> {
        override fun keys(carrier: MessageHeaders): Iterable<String> = carrier.keys

        override fun get(carrier: MessageHeaders?, key: String): String? =
            carrier?.get(key)?.toString()
    }

    /**
     * ConsumerRecord용 TextMapGetter
     */
    private object ConsumerRecordTextMapGetter : TextMapGetter<ConsumerRecord<*, *>> {
        override fun keys(carrier: ConsumerRecord<*, *>): Iterable<String> =
            carrier.headers().map { it.key() }

        override fun get(carrier: ConsumerRecord<*, *>?, key: String): String? =
            carrier?.headers()?.lastHeader(key)?.value()?.toString(Charsets.UTF_8)
    }
}
