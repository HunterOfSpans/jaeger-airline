# OpenTelemetry SDK 기반 분산 추적 구현 가이드

> 작성일: 2026-01-04
> 프로젝트: Jaeger Airline MSA

---

## 1. 왜 OTel SDK 방식을 선택했는가?

### 1.1 Spring Boot 3에서의 선택지

Spring Boot 3에서 분산 추적을 구현하는 방법은 크게 3가지:

| 방식 | 설명 | 제어 수준 |
|------|------|----------|
| **Micrometer Tracing + OTel Bridge** | Spring 추상화 레이어 사용 | 낮음 |
| **OTel Java Agent** | JVM 에이전트로 자동 계측 | 없음 (자동) |
| **OTel SDK 직접 사용** | OpenTelemetry API/SDK 직접 호출 | 높음 |

### 1.2 우리의 선택: OTel SDK + Spring Boot Starter

```kotlin
// build.gradle.kts
implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
implementation("io.micrometer:micrometer-tracing-bridge-otel")
```

**선택 이유:**

1. **Kafka Consumer 커스텀 트레이싱 필요**
   - Spring Kafka의 기본 계측은 Consumer 측 trace context 전파가 불완전
   - `@KafkaListener`에서 parent span 연결을 위해 직접 제어 필요

2. **세밀한 Span 제어**
   - 비즈니스 로직에 맞는 커스텀 span 이름
   - 동적 속성 추가 (reservationId, flightId 등)
   - 조건부 예외 기록

3. **AOP 기반 선언적 트레이싱**
   - `@KafkaOtelTrace` 커스텀 어노테이션으로 깔끔한 코드
   - 비즈니스 로직과 트레이싱 로직 분리

4. **향후 확장성**
   - 다른 메시징 시스템 추가 시 동일 패턴 적용 가능
   - 커스텀 계측 로직 추가 용이

---

## 2. 아키텍처 개요

### 2.1 전체 트레이싱 흐름

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Service A (Producer)                          │
│  ┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐    │
│  │ REST API    │────>│ Business Logic   │────>│ KafkaTemplate     │    │
│  │ (auto span) │     │ (custom span)    │     │ (auto propagation)│    │
│  └─────────────┘     └──────────────────┘     └─────────┬─────────┘    │
└─────────────────────────────────────────────────────────┼──────────────┘
                                                          │
                    Kafka Headers: traceparent, tracestate│
                                                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Kafka Broker                                   │
└─────────────────────────────────────────────────────────┬───────────────┘
                                                          │
                                                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Service B (Consumer)                          │
│  ┌───────────────────┐     ┌──────────────────┐     ┌─────────────┐    │
│  │ @KafkaListener    │────>│ @KafkaOtelTrace  │────>│ Business    │    │
│  │ (message receive) │     │ (context extract)│     │ Logic       │    │
│  └───────────────────┘     └──────────────────┘     └─────────────┘    │
│                                    │                                    │
│                                    ▼                                    │
│                        ┌──────────────────────┐                        │
│                        │ KafkaTracingAspect   │                        │
│                        │ - Extract context    │                        │
│                        │ - Create child span  │                        │
│                        │ - Set attributes     │                        │
│                        └──────────────────────┘                        │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼ OTLP (gRPC :4317)
┌─────────────────────────────────────────────────────────────────────────┐
│                           Jaeger Collector                              │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Trace Context 전파 방식

**W3C Trace Context 표준 사용:**

```
traceparent: 00-{trace-id}-{span-id}-{flags}
tracestate: (optional vendor-specific data)
```

**Producer 측 (자동):**
```kotlin
// KafkaTemplate에서 자동으로 헤더 추가
kafkaTemplate.setObservationEnabled(true)
kafkaTemplate.send("topic", message)
// → Kafka 헤더에 traceparent 자동 삽입
```

**Consumer 측 (커스텀 Aspect):**
```kotlin
// KafkaTracingAspect에서 헤더 추출
val parentContext = propagators.textMapPropagator.extract(
    Context.current(),
    messageHeaders,
    getter
)

// Parent context를 기반으로 새 span 생성
tracer.spanBuilder(spanName)
    .setParent(parentContext)  // ← 여기서 연결
    .startSpan()
```

---

## 3. 핵심 구현 상세

### 3.1 OpenTelemetry 자동 구성

Spring Boot Starter가 제공하는 자동 구성을 활용:

```yaml
# application.yml
otel:
  exporter:
    otlp:
      protocol: grpc
      endpoint: http://collector:4317
  traces:
    exporter: otlp
```

```kotlin
// TracingConfig.kt - 최소 설정
@Configuration
class TracingConfig {
    @Autowired
    private lateinit var openTelemetry: OpenTelemetry

    @Bean
    fun tracer(): Tracer {
        return openTelemetry.getTracer("service-name", "1.0.0")
    }
}
```

### 3.2 @KafkaOtelTrace 어노테이션

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KafkaOtelTrace(
    val spanName: String = "",           // 커스텀 span 이름
    val attributes: Array<String> = [],  // 추가 속성 ["key=value"]
    val recordException: Boolean = true, // 예외 기록 여부
    val recordMessageContent: Boolean = false  // 메시지 내용 기록 (보안 주의)
)
```

**사용 예시:**
```kotlin
@KafkaListener(topics = ["payment.approved"], groupId = "ticket")
@KafkaOtelTrace(
    spanName = "process-payment-approval",
    attributes = ["event.type=payment.approved", "service=ticket"]
)
fun handlePaymentApproval(
    @Payload message: String,
    @Headers headers: MessageHeaders
) {
    // 비즈니스 로직
}
```

### 3.3 KafkaTracingAspect 동작 원리

```kotlin
@Aspect
@Component
class KafkaTracingAspect(private val openTelemetry: OpenTelemetry) {

    @Around("@annotation(kafkaOtelTrace)")
    fun traceKafkaListener(pjp: ProceedingJoinPoint, kafkaOtelTrace: KafkaOtelTrace): Any? {
        // 1. 메서드 파라미터에서 헤더 추출
        val messageHeaders = pjp.args
            .filterIsInstance<MessageHeaders>()
            .firstOrNull()

        // 2. Trace Context 추출 (W3C Trace Context)
        val getter = object : TextMapGetter<MessageHeaders> {
            override fun keys(carrier: MessageHeaders) = carrier.keys
            override fun get(carrier: MessageHeaders?, key: String) =
                carrier?.get(key)?.toString()
        }
        val parentContext = openTelemetry.propagators
            .textMapPropagator
            .extract(Context.current(), messageHeaders, getter)

        // 3. Child Span 생성
        val span = openTelemetry.getTracer("kafka-otel-trace")
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.CONSUMER)
            .setParent(parentContext)  // Parent 연결!
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination", topic)
            .startSpan()

        // 4. Span Context 내에서 실행
        return try {
            span.makeCurrent().use {
                pjp.proceed()
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
    }
}
```

---

## 4. Span 속성 (Semantic Conventions)

OpenTelemetry Semantic Conventions를 따르는 속성들:

| 속성 | 설명 | 예시 |
|------|------|------|
| `messaging.system` | 메시징 시스템 | `kafka` |
| `messaging.destination` | 토픽 이름 | `payment.approved` |
| `messaging.destination_kind` | 목적지 종류 | `topic` |
| `messaging.kafka.partition` | 파티션 번호 | `0` |
| `messaging.kafka.offset` | 오프셋 | `12345` |

**커스텀 속성 추가:**
```kotlin
@KafkaOtelTrace(
    attributes = [
        "reservation.id=\${reservationId}",
        "flight.id=KE001",
        "event.priority=high"
    ]
)
```

---

## 5. Jaeger에서 확인

### 5.1 Trace 조회

1. Jaeger UI 접속: http://localhost:16686
2. Service 선택: `reservation-service`
3. Operation: `POST /v1/reservations` 또는 커스텀 span 이름
4. Find Traces 클릭

### 5.2 기대되는 Trace 구조

```
reservation-service: POST /v1/reservations (500ms)
├── reservation-service: check-flight-availability (50ms)
├── flight-service: POST /v1/flights/{id}/reserve (100ms)
├── reservation-service: kafka-send payment.requested (10ms)
│   └── payment-service: process-payment-request (200ms)  ← Kafka Consumer
│       └── payment-service: kafka-send payment.approved (10ms)
│           └── ticket-service: process-payment-approval (100ms)  ← Kafka Consumer
└── reservation-service: complete-reservation (30ms)
```

---

## 6. 트러블슈팅

### 6.1 Trace가 끊어지는 경우

**증상:** Kafka Consumer에서 새로운 trace가 시작됨 (parent 연결 안 됨)

**원인:** MessageHeaders를 메서드 파라미터로 받지 않음

**해결:**
```kotlin
// ❌ 잘못된 예
@KafkaListener(topics = ["topic"])
@KafkaOtelTrace
fun handle(@Payload message: String) { }

// ✅ 올바른 예
@KafkaListener(topics = ["topic"])
@KafkaOtelTrace
fun handle(
    @Payload message: String,
    @Headers headers: MessageHeaders  // 필수!
) { }
```

### 6.2 디버그 로깅 활성화

```yaml
logging:
  level:
    io.opentelemetry: DEBUG
    com.airline.*.aspect: DEBUG
```

---

## 7. 참고 자료

- [OpenTelemetry Java SDK](https://opentelemetry.io/docs/languages/java/)
- [OpenTelemetry Semantic Conventions - Messaging](https://opentelemetry.io/docs/specs/semconv/messaging/)
- [Spring Boot OpenTelemetry Starter](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/spring/spring-boot-autoconfigure)
- [W3C Trace Context](https://www.w3.org/TR/trace-context/)
