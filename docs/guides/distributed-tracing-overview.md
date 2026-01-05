# 분산 추적 동작 원리 가이드

> **작성일**: 2026-01-06 | **버전**: Jaeger 2.14.1, OpenTelemetry 2.11.0 | **대상**: 개발자

---

## 목차

- [1. 개요](#1-개요)
- [2. 아키텍처](#2-아키텍처)
- [3. 동기 추적 (OpenFeign)](#3-동기-추적-openfeign)
- [4. 비동기 추적 (Kafka)](#4-비동기-추적-kafka)
- [5. 오류 및 보상 트랜잭션 추적](#5-오류-및-보상-트랜잭션-추적)
- [6. 추적 데이터 확인 방법](#6-추적-데이터-확인-방법)
- [7. 구현 상세](#7-구현-상세)

---

## 1. 개요

이 프로젝트는 **OpenTelemetry SDK**와 **Jaeger**를 사용하여 마이크로서비스 간 분산 추적을 구현합니다.

### 1.1 추적 방식

| 통신 방식 | 추적 메커니즘 | 컨텍스트 전파 |
|-----------|--------------|---------------|
| **동기 (HTTP/Feign)** | 자동 계측 (micrometer-tracing) | HTTP 헤더 (`traceparent`) |
| **비동기 (Kafka)** | 수동 계측 (@KafkaOtelTrace AOP) | Kafka 메시지 헤더 |

### 1.2 서비스 구성

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Reservation │────▶│   Flight    │────▶│   Payment   │────▶│   Ticket    │
│   (8083)    │     │   (8080)    │     │   (8082)    │     │   (8081)    │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │                   │
       └───────────────────┴───────────────────┴───────────────────┘
                                   │
                           ┌───────▼───────┐
                           │  Jaeger v2    │
                           │  (Collector)  │
                           │    :4317      │
                           └───────────────┘
```

---

## 2. 아키텍처

### 2.1 추적 데이터 흐름

```
Spring Boot App                    Jaeger
┌──────────────────────┐          ┌──────────────────┐
│  OTel SDK            │   OTLP   │  Collector       │
│  ┌────────────────┐  │  (gRPC)  │  ┌────────────┐  │
│  │ Micrometer     │──┼──────────┼─▶│ OTLP       │  │
│  │ Tracing Bridge │  │  :4317   │  │ Receiver   │  │
│  └────────────────┘  │          │  └─────┬──────┘  │
└──────────────────────┘          │        │         │
                                  │  ┌─────▼──────┐  │
                                  │  │ Jaeger     │  │
                                  │  │ Storage    │  │
                                  │  │ Exporter   │  │
                                  │  └─────┬──────┘  │
                                  └────────┼─────────┘
                                           │
                                  ┌────────▼─────────┐
                                  │  Elasticsearch   │
                                  │     :9200        │
                                  └──────────────────┘
```

### 2.2 핵심 컴포넌트

| 컴포넌트 | 역할 |
|---------|------|
| **micrometer-tracing-bridge-otel** | Spring의 관측 API를 OTel SDK로 브릿지 |
| **opentelemetry-exporter-otlp** | 추적 데이터를 OTLP 프로토콜로 전송 |
| **@KafkaOtelTrace** | Kafka 메시지의 커스텀 추적 어노테이션 |
| **KafkaTracingAspect** | Kafka 컨텍스트 전파를 위한 AOP |

---

## 3. 동기 추적 (OpenFeign)

### 3.1 동작 원리

OpenFeign 호출은 **자동 계측**됩니다. `feign-micrometer` 의존성이 자동으로 HTTP 헤더에 추적 컨텍스트를 주입합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        단일 Trace ID                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Reservation          Flight            Payment           Ticket   │
│  ┌─────────┐         ┌─────────┐       ┌─────────┐      ┌─────────┐│
│  │ Span 1  │────────▶│ Span 2  │──────▶│ Span 3  │─────▶│ Span 4  ││
│  │ POST    │  HTTP   │ GET     │ HTTP  │ POST    │ HTTP │ POST    ││
│  │ /feign  │ Header  │ /flight │Header │/payment │Header│ /ticket ││
│  └─────────┘         └─────────┘       └─────────┘      └─────────┘│
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 테스트 엔드포인트

| 엔드포인트 | 설명 | 호출 체인 |
|-----------|------|----------|
| `POST /v1/tracing/feign/simple-flow` | 간단한 동기 호출 | Reservation → Flight → Payment → Ticket |
| `POST /v1/tracing/feign/complex-flow` | Circuit Breaker 포함 | 동일 + 장애 복원력 테스트 |
| `POST /v1/tracing/feign/parallel-calls` | 병렬 호출 | Reservation → (Flight1 \|\| Flight2) |

### 3.3 HTTP 헤더 전파

```http
GET /v1/flights/KE001 HTTP/1.1
Host: flight:8080
traceparent: 00-657551c16069312a0e288bb7a8b499cf-0e288bb7a8b499cf-01
tracestate:
```

- `traceparent`: W3C Trace Context 표준 헤더
- 형식: `version-traceId-parentSpanId-flags`

### 3.4 Jaeger에서 확인되는 Operation

```
reservation-service:
  - http post /v1/tracing/feign/simple-flow
  - HTTP GET (Feign client call)
  - HTTP POST (Feign client call)

flight-service:
  - GET /v1/flights/{flightId}
  - POST /v1/flights/{flightId}/availability

payment-service:
  - POST /v1/payments

ticket-service:
  - POST /v1/tickets
```

---

## 4. 비동기 추적 (Kafka)

### 4.1 동작 원리

Kafka 메시지는 HTTP와 달리 **수동 계측**이 필요합니다. `@KafkaOtelTrace` 어노테이션과 AOP를 통해 구현됩니다.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              단일 Trace ID                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Reservation        Flight           Payment          Ticket      Reservation│
│  ┌─────────┐       ┌─────────┐      ┌─────────┐     ┌─────────┐   ┌─────────┐│
│  │ Span 1  │──────▶│ Span 2  │─────▶│ Span 3  │────▶│ Span 4  │──▶│ Span 5  ││
│  │ send    │ Kafka │ process │Kafka │ process │Kafka│ process │Kfk│ process ││
│  │ reserv. │ Header│ reserv. │Header│ seat.   │Hdr  │ payment │Hdr│ ticket  ││
│  │ request │       │ request │      │ reserved│     │ approved│   │ issued  ││
│  └─────────┘       └─────────┘      └─────────┘     └─────────┘   └─────────┘│
│                                                                             │
│  Topic:            Topic:           Topic:          Topic:        Topic:    │
│  reservation.      seat.            payment.        ticket.       reserv.   │
│  requested         reserved         approved        issued        completed │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 이벤트 체인

```
reservation.requested → seat.reserved → payment.approved → ticket.issued → reservation.completed
     (Reservation)        (Flight)         (Payment)         (Ticket)        (Reservation)
```

### 4.3 테스트 엔드포인트

| 엔드포인트 | 설명 |
|-----------|------|
| `POST /v1/tracing/kafka/simple-events` | 기본 이벤트 체인 |
| `POST /v1/tracing/kafka/complex-events` | 복잡한 이벤트 플로우 |
| `POST /v1/tracing/kafka/failure-compensation` | 실패/보상 트랜잭션 |
| `POST /v1/tracing/kafka/multi-topic-events` | 다중 토픽 병렬 발행 |

### 4.4 컨텍스트 전파 구현

#### Producer 측 (컨텍스트 주입)

```kotlin
// KafkaTracingAspect.kt
val currentContext = Context.current()
val propagator = GlobalOpenTelemetry.getPropagators().textMapPropagator

propagator.inject(currentContext, headers) { carrier, key, value ->
    carrier?.add(RecordHeader(key, value.toByteArray()))
}
```

#### Consumer 측 (컨텍스트 추출)

```kotlin
@KafkaListener(topics = ["seat.reserved"])
@KafkaOtelTrace(spanName = "process-seat-reserved")
fun seatReservedListener(@Payload message: String, @Headers headers: MessageHeaders) {
    // AOP가 헤더에서 traceparent를 추출하여 현재 Span에 연결
}
```

### 4.5 Kafka 메시지 헤더

```
Headers:
  traceparent: 00-ce9aea8b511cf1cb69dcac2d8a384a8b-69dcac2d8a384a8b-01
  tracestate:
```

### 4.6 Jaeger에서 확인되는 Operation

```
reservation-service:
  - kafka-simple-event-chain
  - reservation.requested send
  - ticket.issued process
  - reservation.completed send

flight-service:
  - reservation.requested process
  - seat.reserved send

payment-service:
  - seat.reserved process
  - payment.approved send

ticket-service:
  - payment.approved process
  - ticket.issued send
```

---

## 5. 오류 및 보상 트랜잭션 추적

### 5.1 오류 추적

오류 발생 시 Span에 에러 정보가 기록됩니다.

```
┌─────────────────────────────────────────────────────────────────┐
│  Trace with Error                                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Reservation         Flight          Payment                    │
│  ┌─────────┐        ┌─────────┐     ┌─────────┐                │
│  │ Span 1  │───────▶│ Span 2  │────▶│ Span 3  │                │
│  │ ✓ OK    │        │ ✓ OK    │     │ ✗ ERROR │                │
│  └─────────┘        └─────────┘     └─────────┘                │
│                                           │                     │
│                                     error=true                  │
│                                     error.message="결제 실패"   │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 보상 트랜잭션 추적

실패 시 보상 이벤트가 발행되고 추적됩니다.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Failure & Compensation Flow                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  정상 흐름:                                                                  │
│  reservation.requested → seat.reserved → payment.approved                   │
│                                               │                             │
│                                          결제 실패!                          │
│                                               │                             │
│  보상 흐름:                                    ▼                             │
│                                         payment.failed                      │
│                                               │                             │
│                                               ▼                             │
│                                      compensation.executed                  │
│                                      (좌석 해제, 예약 취소)                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 오류 케이스 테스트

```bash
# 실패/보상 트랜잭션 테스트
curl -X POST http://localhost:8083/v1/tracing/kafka/failure-compensation

# Circuit Breaker 테스트
curl -X POST http://localhost:8083/v1/tracing/feign/circuit-breaker-test
```

### 5.4 Jaeger에서 확인되는 오류 Operation

```
reservation-service:
  - kafka-failure-compensation-flow
  - payment.failed send

Jaeger UI에서 확인:
  - 빨간색으로 표시된 에러 Span
  - error=true 태그
  - 에러 로그 및 스택 트레이스
```

---

## 6. 추적 데이터 확인 방법

### 6.1 Jaeger UI

**URL**: http://localhost:16686

1. **Service** 선택: `reservation`, `flight`, `payment`, `ticket`
2. **Operation** 선택 (예시):
   - `http post /v1/tracing/feign/simple-flow`
   - `kafka-simple-event-chain`
3. **Find Traces** 클릭

### 6.2 Jaeger API

```bash
# 등록된 서비스 목록
curl http://localhost:16686/api/services

# 서비스별 Operation 목록
curl http://localhost:16686/api/operations?service=reservation

# 최근 Trace 조회
curl "http://localhost:16686/api/traces?service=reservation&limit=10"

# 에러 Trace만 조회
curl "http://localhost:16686/api/traces?service=reservation&tags={\"error\":\"true\"}"
```

### 6.3 Trace 분석 예시

```bash
# 트레이스의 서비스 연결 확인
curl -s "http://localhost:16686/api/traces?service=reservation&limit=1" | \
  jq '.data[0] | {
    traceID,
    spanCount: (.spans | length),
    services: [.processes | to_entries[] | .value.serviceName] | unique
  }'
```

출력 예시:
```json
{
  "traceID": "657551c16069312a0e288bb7a8b499cf",
  "spanCount": 33,
  "services": ["flight", "payment", "reservation", "ticket"]
}
```

---

## 7. 구현 상세

### 7.1 의존성 (build.gradle.kts)

```kotlin
dependencies {
    // Micrometer Tracing + OTel Bridge
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Feign 자동 계측
    implementation("io.github.openfeign:feign-micrometer")

    // Kafka 수동 계측 (공통 라이브러리)
    implementation("com.airline:kafka-tracing")
}
```

### 7.2 application.yml 설정

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 모든 요청 추적 (운영: 0.1 권장)
  otlp:
    tracing:
      endpoint: http://jaeger-collector:4318/v1/traces

spring:
  application:
    name: reservation  # Jaeger에 표시될 서비스 이름
```

### 7.3 @KafkaOtelTrace 어노테이션

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KafkaOtelTrace(
    val spanName: String = "",
    val attributes: Array<String> = [],
    val recordMessageContent: Boolean = false
)
```

### 7.4 KafkaTracingAspect (핵심 로직)

```kotlin
@Around("@annotation(kafkaOtelTrace)")
fun traceKafkaMessage(joinPoint: ProceedingJoinPoint, kafkaOtelTrace: KafkaOtelTrace): Any? {
    // 1. 메시지 헤더에서 부모 컨텍스트 추출
    val parentContext = extractContext(headers)

    // 2. 새 Span 생성 (부모와 연결)
    val span = tracer.spanBuilder(spanName)
        .setParent(parentContext)
        .startSpan()

    // 3. Span 컨텍스트에서 실행
    return span.makeCurrent().use {
        try {
            joinPoint.proceed()
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

## 부록: 테스트 명령어 모음

```bash
# OpenFeign 동기 추적 테스트
./script/test-feign-tracing.sh

# Kafka 비동기 추적 테스트
./script/test-kafka-tracing.sh

# 개별 엔드포인트 테스트
curl -X POST http://localhost:8083/v1/tracing/feign/simple-flow
curl -X POST http://localhost:8083/v1/tracing/kafka/simple-events
curl -X POST http://localhost:8083/v1/tracing/kafka/failure-compensation

# Jaeger UI
open http://localhost:16686
```
