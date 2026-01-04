package com.airline.tracing.config

import com.airline.tracing.aspect.KafkaTracingAspect
import io.opentelemetry.api.OpenTelemetry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.KafkaListener

/**
 * Kafka Tracing 자동 설정
 *
 * OpenTelemetry와 Spring Kafka가 클래스패스에 있을 때 자동으로 활성화됩니다.
 *
 * 비활성화하려면:
 * ```yaml
 * kafka.tracing.enabled: false
 * ```
 */
@AutoConfiguration
@ConditionalOnClass(OpenTelemetry::class, KafkaListener::class)
@ConditionalOnProperty(
    prefix = "kafka.tracing",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true  // 기본값: 활성화
)
class KafkaTracingAutoConfiguration {

    private val logger = LoggerFactory.getLogger(KafkaTracingAutoConfiguration::class.java)

    @Bean
    @ConditionalOnBean(OpenTelemetry::class)
    @ConditionalOnMissingBean(KafkaTracingAspect::class)
    fun kafkaTracingAspect(openTelemetry: OpenTelemetry): KafkaTracingAspect {
        logger.info("Kafka Tracing enabled with OpenTelemetry")
        return KafkaTracingAspect(openTelemetry)
    }
}
