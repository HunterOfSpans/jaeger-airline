package com.airline.ticket.aspect

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component


@Aspect
@Component
class KafkaTracingAspect (private val openTelemetry: OpenTelemetry){

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    fun traceKafkaListener(pjp: ProceedingJoinPoint): Any? {
        val record = pjp.args.filterIsInstance<ConsumerRecord<*, *>>().firstOrNull() ?: return pjp.proceed()

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

        // 새 Span 시작
        val tracer = openTelemetry.getTracer("kafka-listener")
        val spanName = String.format("KafkaConsumer.%s", pjp.signature.name)
        val span = tracer.spanBuilder(spanName)
            .setSpanKind(SpanKind.CONSUMER)
            .setParent(parentContext)
            .startSpan()

        return try {
            span.makeCurrent().use {
                pjp.proceed()
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.message ?: "error")
            throw e
        } finally {
            span.end()
        }
    }

}