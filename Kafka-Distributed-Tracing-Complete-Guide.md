# Kafka 분산 추적 완전 가이드

## 개요

이 문서는 Apache Kafka와 OpenTelemetry를 사용한 분산 추적의 모든 것을 다룹니다. OpenFeign과 달리 **Kafka는 자동 계측이 제한적**이며, 완전한 분산 추적을 위해서는 **수동 구현이 필요**합니다. 본 가이드는 현재 프로젝트에서 구현된 커스텀 어노테이션과 AOP 기반 솔루션을 상세히 분석하고, 모범 사례를 제공합니다.

## 핵심 차이점: OpenFeign vs Kafka

### OpenFeign (HTTP 기반)
- ✅ **완전 자동화**: feign-micrometer만 추가하면 자동 트레이스 전파
- ✅ **W3C 표준**: HTTP 헤더로 traceparent/tracestate 자동 처리
- ✅ **제로 코드**: 별도 구현 불필요

### Kafka (메시징 기반)
- ⚠️ **제한적 자동화**: 2024년 기준 Spring Boot Starter로도 Consumer 쪽 완전 자동화 어려움
- ⚠️ **Producer만 자동**: observationEnabled(true)로 Producer는 자동, Consumer는 수동 필요
- ⚠️ **수동 헤더 처리**: 메시지 헤더에 트레이스 컨텍스트 수동 삽입/추출 필요
- ⚠️ **커스텀 구현 필수**: @KafkaListener에는 자동 계측이 없어서 AOP로 직접 구현해야 함

## Kafka 분산 추적 아키텍처

### 1. 전체 플로우

```
[Producer Service]
    ↓ 1. 현재 Span Context 추출
    ↓ 2. Kafka 메시지 헤더에 삽입
    ↓
[Kafka Broker] → 메시지 + 트레이스 헤더
    ↓
[Consumer Service]  
    ↓ 3. 메시지 헤더에서 Trace Context 추출
    ↓ 4. 새 Child Span 생성
    ↓ 5. 비즈니스 로직 실행
```

### 2. 현재 프로젝트 구현 방식

#### A. Producer 쪽: 자동 헤더 삽입
```kotlin
@Bean
fun kafkaTemplate(): KafkaTemplate<String, String> {
    val kafkaTemplate = KafkaTemplate(producerFactory())
    kafkaTemplate.setObservationEnabled(true)  // ← 핵심! 자동 헤더 삽입 활성화
    return kafkaTemplate
}
```

#### B. Consumer 쪽: 커스텀 어노테이션 + AOP
```kotlin
@KafkaListener(topics = ["ticket.issued"], groupId = "reservation")
@KafkaOtelTrace(
    spanName = "process-ticket-issued",
    attributes = ["event.type=ticket.issued", "service=reservation"]
)
fun ticketIssuedListener(
    @Payload message: String,
    @Headers headers: MessageHeaders
) {
    // 깔끔한 비즈니스 로직 - 메시지만 처리
    println(message)
    reservationService.confirm()
}
```

## Producer 쪽: 자동 헤더 전파

### 1. observationEnabled의 핵심 역할

**⚠️ 중요: observationEnabled(true) 설정 없으면 Producer 추적 안됨!**

```kotlin
// ❌ 이 설정 없으면 추적 안됨
val kafkaTemplate = KafkaTemplate(producerFactory())

// ✅ 이 설정으로 자동 추적 활성화 (필수!)
kafkaTemplate.setObservationEnabled(true)
```

이 **단 한 줄** 설정으로 Spring Boot가 자동으로:
- 현재 활성화된 Span의 Trace Context 추출
- Kafka 메시지 헤더에 W3C 호환 트레이스 정보 삽입
- Producer 쪽 Span 생성 및 관리

### 2. 실제 헤더 내용

Kafka 메시지에 다음 헤더가 자동으로 추가됩니다:

```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
tracestate: (벤더별 추가 정보)
```

### 3. Producer 구성 예시

```kotlin
@Configuration
class KafkaProducerConfig {
    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "kafka:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3
            // 트레이싱 관련 별도 설정 불필요
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory()).apply {
            setObservationEnabled(true)  // ← 이것만 추가하면 자동 헤더 삽입
        }
    }
}
```

## Consumer 쪽: 커스텀 구현 방식

### ⚠️ 왜 Consumer는 수동 구현이 필요한가?

**@KafkaListener는 자동 계측이 안됩니다!**

```kotlin
// ❌ 이렇게 하면 새로운 트레이스가 시작됨 (연결 안됨)
@KafkaListener(topics = ["test.topic"])
fun listener(message: String) {
    // Producer에서 보낸 트레이스 컨텍스트와 연결되지 않음
}
```

**문제점:**
1. Spring Boot Starter도 @KafkaListener 자동 계측 지원 안함
2. 메시지 헤더에서 트레이스 컨텍스트 자동 추출 안됨  
3. Child Span 자동 생성 안됨
4. 결과: **끊어진 트레이스** (Producer ↛ Consumer)

**해결책: 커스텀 어노테이션 + AOP**

### 1. @KafkaOtelTrace 커스텀 어노테이션

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class KafkaOtelTrace(
    val spanName: String = "",                    // 커스텀 span 이름
    val attributes: Array<String> = [],           // 추가 속성
    val recordException: Boolean = true,          // 예외 기록 여부
    val recordMessageContent: Boolean = false     // 메시지 내용 기록 여부
)
```

### 2. KafkaTracingAspect 구현 핵심

#### A. 메시지 헤더에서 Trace Context 추출
```kotlin
@Around("@annotation(kafkaOtelTrace)")
fun traceKafkaListener(pjp: ProceedingJoinPoint, kafkaOtelTrace: KafkaOtelTrace): Any? {
    val record = pjp.args.filterIsInstance<ConsumerRecord<*, *>>().firstOrNull()
    
    // TextMapGetter로 헤더에서 트레이스 정보 추출
    val getter = object : TextMapGetter<ConsumerRecord<*, *>> {
        override fun keys(carrier: ConsumerRecord<*, *>): Iterable<String> =
            carrier.headers().map { it.key() }

        override fun get(carrier: ConsumerRecord<*, *>?, key: String): String? =
            carrier?.headers()?.lastHeader(key)?.value()?.toString(Charsets.UTF_8)
    }

    // OpenTelemetry Propagator로 부모 컨텍스트 복원
    val parentContext = openTelemetry.propagators.textMapPropagator
        .extract(Context.current(), record, getter)
```

#### B. Child Span 생성 및 컨텍스트 연결
```kotlin
    val spanName = if (kafkaOtelTrace.spanName.isNotBlank()) {
        kafkaOtelTrace.spanName
    } else {
        "KafkaConsumer.${pjp.signature.name}"
    }

    val span = tracer.spanBuilder(spanName)
        .setSpanKind(SpanKind.CONSUMER)
        .setParent(parentContext)                    // ← 부모 컨텍스트 연결
        .setAttribute("messaging.system", "kafka")
        .setAttribute("messaging.destination", record.topic())
        .setAttribute("messaging.kafka.partition", record.partition().toLong())
        .setAttribute("messaging.kafka.offset", record.offset())
        .startSpan()

    return try {
        span.makeCurrent().use {                     // ← 현재 스레드에 컨텍스트 설정
            pjp.proceed()                            // 실제 리스너 메서드 실행
        }
    } finally {
        span.end()
    }
```

### 3. 실제 사용 예시

```kotlin
@Component
class TicketListener(private val reservationService: ReservationService) {

    @KafkaListener(topics = ["ticket.issued"], groupId = "reservation")
    @KafkaOtelTrace(
        spanName = "process-ticket-issued",
        attributes = ["event.type=ticket.issued", "service=reservation"],
        recordMessageContent = false  // 보안상 메시지 내용은 기록하지 않음
    )
    fun ticketIssuedListener(record: ConsumerRecord<String, String>) {
        println(record.value())
        reservationService.confirm()  // ← 이 호출도 같은 트레이스에 포함됨
    }
}
```

## OpenTelemetry 자동 계측 현황 (2024년)

### 1. Spring Boot Starter 지원 범위

```yaml
# 2024년 현재 지원되는 자동 계측
otel.instrumentation.kafka.enabled: true  # Producer 쪽 기본 지원
```

**지원되는 것:**
- ✅ Kafka Producer 자동 계측 (observationEnabled=true 시)
- ✅ 기본적인 메트릭 수집
- ✅ Producer 쪽 헤더 자동 삽입

**지원되지 않는 것:**
- ❌ Consumer 쪽 완전 자동 계측
- ❌ @KafkaListener 자동 Span 생성
- ❌ Consumer 쪽 헤더 자동 추출

### 2. Java Agent vs Spring Boot Starter

#### Java Agent 방식
```bash
-javaagent:opentelemetry-javaagent.jar
-Dotel.instrumentation.kafka.enabled=true
```
- ✅ Producer/Consumer 모두 자동 계측
- ✅ 코드 수정 불필요
- ❌ 런타임 오버헤드
- ❌ 세밀한 제어 어려움

#### Spring Boot Starter 방식 (현재 프로젝트)
```gradle
implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter'
```
- ✅ 가벼운 오버헤드
- ✅ 세밀한 제어 가능
- ✅ Spring Boot 통합
- ❌ Consumer 쪽 수동 구현 필요

## 헤더 전파 메커니즘 상세

### 1. Producer → Kafka 헤더 삽입

```kotlin
// KafkaTemplate의 observationEnabled가 true일 때 자동으로 수행
kafkaTemplate.send("topic-name", "key", "message")

// 실제로 전송되는 헤더:
// traceparent: 00-{32자리-trace-id}-{16자리-span-id}-{2자리-flags}
// tracestate: {vendor-specific-data}
```

### 2. Kafka → Consumer 헤더 추출

```kotlin
// AOP에서 자동으로 수행
val getter = object : TextMapGetter<ConsumerRecord<*, *>> {
    override fun get(carrier: ConsumerRecord<*, *>?, key: String): String? =
        carrier?.headers()?.lastHeader(key)?.value()?.toString(Charsets.UTF_8)
}

val parentContext = openTelemetry.propagators.textMapPropagator
    .extract(Context.current(), record, getter)
```

### 3. 트레이스 연결 확인

Jaeger UI에서 다음과 같이 연결된 트레이스를 확인할 수 있습니다:

```
Root Span: HTTP Request (reservation-service)
├─ Child Span: Kafka Producer (kafka.send)
    ├─ Child Span: KafkaConsumer.process-ticket-issued (ticket-service)
        └─ Child Span: Database Query (ticket creation)
```

## AOP 구현 개선 방안

### 다중 매개변수 지원하는 KafkaTracingAspect

현재 구현을 확장하여 여러 매개변수 타입을 지원할 수 있습니다:

```kotlin
@Around("@annotation(kafkaOtelTrace)")
fun traceKafkaListener(pjp: ProceedingJoinPoint, kafkaOtelTrace: KafkaOtelTrace): Any? {
    // 1. ConsumerRecord 우선 탐지
    val record = pjp.args.filterIsInstance<ConsumerRecord<*, *>>().firstOrNull()
    if (record != null) {
        return traceWithConsumerRecord(pjp, kafkaOtelTrace, record)
    }
    
    // 2. MessageHeaders 탐지  
    val messageHeaders = pjp.args.filterIsInstance<MessageHeaders>().firstOrNull()
    if (messageHeaders != null) {
        return traceWithMessageHeaders(pjp, kafkaOtelTrace, messageHeaders)
    }
    
    // 3. ConsumerRecordMetadata 탐지
    val metadata = pjp.args.filterIsInstance<ConsumerRecordMetadata>().firstOrNull()
    if (metadata != null) {
        return traceWithMetadata(pjp, kafkaOtelTrace, metadata)
    }
    
    // 4. 헤더 접근 불가 시 경고
    logger.warn("@KafkaOtelTrace used without supported header parameter types")
    return pjp.proceed()
}

private fun traceWithMessageHeaders(
    pjp: ProceedingJoinPoint, 
    kafkaOtelTrace: KafkaOtelTrace, 
    headers: MessageHeaders
): Any? {
    val getter = object : TextMapGetter<MessageHeaders> {
        override fun keys(carrier: MessageHeaders): Iterable<String> = carrier.keys
        override fun get(carrier: MessageHeaders?, key: String): String? = 
            carrier?.get(key)?.toString()
    }
    
    val parentContext = openTelemetry.propagators.textMapPropagator
        .extract(Context.current(), headers, getter)
        
    // 나머지 Span 생성 로직...
}
```

### 장단점 비교

| 방법 | 장점 | 단점 | 권장도 |
|------|------|------|--------|
| **@Payload + @Headers** | 관심사 분리, 깔끔함, 테스트 쉬움 | - | ⭐⭐⭐⭐⭐ |
| **ConsumerRecord** | 모든 정보 접근, 기존 코드 호환 | 비즈니스 로직 복잡, 강결합 | ⭐⭐⭐ |
| **ConsumerRecordMetadata** | 메타데이터 풍부 | Spring Kafka 2.5+ 필요 | ⭐⭐⭐ |
| **개별 @Header** | 명시적 | AOP 복잡, 트레이스 헤더 하드코딩 | ⭐⭐ |

## 고급 구현 패턴

### 1. 멀티 토픽 이벤트 처리

```kotlin
fun triggerMultiTopicEvents(): List<String> {
    val baseEventId = "MULTI-${UUID.randomUUID()}"
    val span = tracer.spanBuilder("kafka-multi-topic-events")
        .setAttribute("base.event.id", baseEventId)
        .startSpan()
    
    val topics = mapOf(
        "reservation.analytics" to "ANALYTICS_DATA",
        "payment.audit" to "AUDIT_LOG", 
        "ticket.notification" to "NOTIFICATION_REQUEST"
    )
    
    // 모든 토픽에 같은 트레이스 컨텍스트로 이벤트 발행
    topics.forEach { (topic, eventType) ->
        kafkaTemplate.send(topic, eventId, eventMessage) // 자동 헤더 전파
    }
}
```

### 2. 실패 및 보상 트랜잭션 추적

```kotlin
@KafkaListener(topics = ["payment.failed"])
@KafkaOtelTrace(
    spanName = "handle-payment-failure",
    attributes = ["compensation=true", "transaction.type=saga"]
)
fun handlePaymentFailure(record: ConsumerRecord<String, String>) {
    // 보상 트랜잭션 실행
    // 모든 보상 작업이 같은 트레이스에 연결됨
}
```

### 3. 비동기 이벤트 체인 모니터링

```kotlin
@Service
class KafkaTracingService(private val tracer: Tracer) {
    
    fun triggerEventChain(): String {
        val eventId = generateEventId()
        val span = tracer.spanBuilder("kafka-event-chain")
            .setAttribute("event.id", eventId)
            .startSpan()
        
        try {
            kafkaTemplate.send("reservation.requested", eventId, eventData)
            // → flight 서비스에서 자동으로 같은 트레이스로 연결됨
            // → payment 서비스에서 자동으로 같은 트레이스로 연결됨  
            // → ticket 서비스에서 자동으로 같은 트레이스로 연결됨
        } finally {
            span.end()
        }
        
        return eventId
    }
}
```

## 성능 및 모범 사례

### 1. Span 속성 최적화

```kotlin
@KafkaOtelTrace(
    spanName = "process-high-volume-events",
    attributes = ["priority=high", "batch.enabled=true"],
    recordMessageContent = false,  // 고용량 처리 시 비활성화
    recordException = true         // 에러 추적은 항상 활성화
)
```

### 2. 샘플링 전략

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% 샘플링 (고용량 Kafka 처리 시)

# 또는 환경변수로
otel.traces.sampler: parent_based_traceid_ratio
otel.traces.sampler.arg: 0.1
```

### 3. 메모리 관리

```kotlin
// KafkaTracingService에서 이벤트 상태 관리
private val eventStatusMap = ConcurrentHashMap<String, Map<String, Any>>()

// 주기적으로 만료된 이벤트 정리
@Scheduled(fixedRate = 300_000) // 5분마다
fun cleanupExpiredEvents() {
    val now = System.currentTimeMillis()
    eventStatusMap.entries.removeIf { (_, status) ->
        val startTime = status["startTime"] as? Long ?: 0
        (now - startTime) > 600_000 // 10분 후 제거
    }
}
```

## 트러블슈팅 가이드

### 1. 트레이스가 연결되지 않는 경우

**문제**: Consumer에서 새로운 트레이스가 생성됨
**원인**: 
- @KafkaOtelTrace 어노테이션 누락
- KafkaTracingAspect 빈 등록 실패

**해결**:
```kotlin
// 1. 어노테이션 추가 확인
@KafkaListener(topics = ["test.topic"])
@KafkaOtelTrace  // ← 필수

// 2. Aspect 빈 등록 확인
@Aspect
@Component  // ← 필수
class KafkaTracingAspect
```

### 2. Producer 헤더가 전파되지 않는 경우

**문제**: Kafka 메시지에 traceparent 헤더가 없음  
**원인**: `observationEnabled(true)` 설정 누락 - **가장 흔한 실수!**

**진단 방법**:
```kotlin
// 현재 설정 확인
val isEnabled = kafkaTemplate.isObservationEnabled
logger.info("KafkaTemplate observation enabled: {}", isEnabled)
```

**해결**:
```kotlin
@Bean
fun kafkaTemplate(): KafkaTemplate<String, String> {
    return KafkaTemplate(producerFactory()).apply {
        setObservationEnabled(true)  // ← 이 설정이 없으면 100% 추적 안됨!
    }
}
```

**중요**: 이 설정 없이는 다른 모든 OpenTelemetry 설정이 완벽해도 Producer 추적이 작동하지 않습니다.

### 3. Consumer에서 헤더 추출 실패

**문제**: AOP에서 NullPointerException  
**원인**: 헤더 정보에 접근할 수 있는 매개변수가 없음

**현재 구현에서 지원되는 방법들:**

#### 방법 1: @Payload + @Headers 사용 (⭐ 권장)
```kotlin
// ✅ 가장 깔끔하고 추천하는 방법
@KafkaListener(topics = ["test"])
@KafkaOtelTrace
fun listener(
    @Payload message: String,
    @Headers headers: MessageHeaders
) {
    // 비즈니스 로직은 message만 사용
    // 트레이싱은 AOP가 headers로 처리
}
```

#### 방법 2: ConsumerRecord 사용 (호환성)
```kotlin
// ✅ 기존 코드 호환성을 위해 지원
@KafkaListener(topics = ["test"])
@KafkaOtelTrace
fun listener(record: ConsumerRecord<String, String>) {
    val message = record.value()
    val headers = record.headers() 
    // 모든 정보가 record에 포함되지만 비즈니스 로직이 복잡해짐
}
```

#### 방법 3: ConsumerRecordMetadata 사용 (Spring Kafka 2.5+)
```kotlin
// ✅ 헤더 + 메타데이터만 필요한 경우
@KafkaListener(topics = ["test"])
@KafkaOtelTrace  
fun listener(
    message: String,
    meta: ConsumerRecordMetadata
) {
    // AOP에서 ConsumerRecordMetadata 매개변수 탐지하도록 수정 필요
}
```

**❌ 지원하지 않는 방법:**
```kotlin
// 현재 AOP 구현으로는 추적 불가
@KafkaOtelTrace
fun wrongListener(message: String) { }

// 개별 헤더 접근도 현재 구현으로는 복잡
@KafkaOtelTrace
fun complexListener(
    @Payload message: String,
    @Header("traceparent") traceparent: String?
) { }
```

**현재 KafkaTracingAspect는 ConsumerRecord만 지원**하므로, 다른 방법을 사용하려면 AOP 코드 수정이 필요합니다.

### 4. 디버깅 도구

```kotlin
// 헤더 내용 확인용 인터셉터
@Component
class KafkaHeaderDebugInterceptor : ConsumerInterceptor<String, String> {
    override fun onConsume(records: ConsumerRecords<String, String>): ConsumerRecords<String, String> {
        records.forEach { record ->
            record.headers().forEach { header ->
                if (header.key().startsWith("trace")) {
                    log.info("Kafka Header - {}: {}", header.key(), String(header.value()))
                }
            }
        }
        return records
    }
}
```

## 모니터링 및 관찰성

### 1. Jaeger UI에서 Kafka 트레이스 확인

1. **Service 선택**: 각 마이크로서비스 선택
2. **Operation 확인**: 
   - Producer: `kafka.send`
   - Consumer: `KafkaConsumer.{method-name}`
3. **Timeline 분석**: 메시지 처리 시간, 대기 시간 확인

### 2. 핵심 메트릭 모니터링

```kotlin
// 커스텀 메트릭 추가 예시
@KafkaOtelTrace(
    attributes = [
        "kafka.consumer.group=reservation",
        "kafka.topic=ticket.issued", 
        "processing.priority=high"
    ]
)
```

### 3. 알림 및 SLA 모니터링

- **처리 지연**: Consumer Lag 모니터링
- **에러율**: 실패한 메시지 처리 비율
- **처리량**: 초당 처리되는 메시지 수

## 결론 및 권장사항

### Kafka 분산 추적의 핵심 원칙

1. **Producer는 자동, Consumer는 수동**: 현재 가장 실용적인 접근법
2. **observationEnabled(true) 필수**: Producer 자동 추적의 핵심 설정
3. **커스텀 어노테이션 활용**: @KafkaOtelTrace로 일관된 추적 구현
4. **AOP 기반 구현**: 비즈니스 로직과 추적 로직 분리
5. **세밀한 제어**: 메시지 내용 기록, 에러 처리 등 옵션 제공

### ⚠️ 가장 중요한 설정

```kotlin
// Producer 자동 추적 활성화 (절대 빼먹지 말 것!)
kafkaTemplate.setObservationEnabled(true)
```

### OpenFeign vs Kafka 비교 결론

| 측면 | OpenFeign | Kafka |
|------|-----------|-------|
| **자동화 수준** | 완전 자동 | **부분 자동** (Producer만, Consumer 수동) |
| **구현 복잡도** | 매우 단순 | **중간** (@KafkaListener는 직접 구현 필요) |
| **필요한 설정** | feign-micrometer만 | **Producer**: observationEnabled, **Consumer**: AOP |
| **제어 수준** | 제한적 | 세밀함 |
| **성능 오버헤드** | 최소 | 약간 |
| **유지보수** | 쉬움 | 보통 |
| **학습 곡선** | 낮음 | **높음** (OpenTelemetry API 이해 필요) |

### 미래 전망

- **Spring Boot 3.x**: Consumer 쪽 자동 계측 개선 예상
- **OpenTelemetry 발전**: Kafka 자동 계측 범위 확대
- **Kafka Streams**: 복잡한 스트림 처리에서의 추적 개선

현재로서는 **Producer 자동 + Consumer 수동** 하이브리드 방식이 가장 실용적이며, 본 가이드의 구현 패턴을 따르면 완전한 Kafka 분산 추적을 달성할 수 있습니다.

## 참고 문헌

- [OpenTelemetry Kafka Instrumentation](https://opentelemetry.io/blog/2022/instrument-kafka-clients/)
- [Spring Boot OpenTelemetry Starter](https://opentelemetry.io/blog/2024/spring-starter-stable/)
- [Kafka Tracing Best Practices](https://medium.com/@ahmadalammar/integrating-spring-kafka-and-opentelemetry-for-effective-distributed-tracing-73aa4748011e)