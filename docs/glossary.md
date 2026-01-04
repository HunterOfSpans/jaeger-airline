# 용어집 (Glossary)

> **작성일**: 2026-01-04 | **버전**: 1.0 | **난이도**: 입문

이 문서는 Jaeger 항공 예약 시스템에서 사용되는 주요 기술 용어를 정의합니다.

---

## 목차

- [분산 추적 (Distributed Tracing)](#분산-추적-distributed-tracing)
- [OpenTelemetry](#opentelemetry)
- [Jaeger](#jaeger)
- [Kafka](#kafka)
- [Spring Boot & Cloud](#spring-boot--cloud)
- [Elasticsearch](#elasticsearch)
- [아키텍처 패턴](#아키텍처-패턴)
- [보안](#보안)

---

## 분산 추적 (Distributed Tracing)

### Trace (트레이스)
하나의 요청이 시스템을 통과하는 전체 경로를 나타내는 데이터 구조. 여러 서비스에 걸친 모든 작업을 하나의 고유한 `Trace ID`로 연결합니다.

```
예시: 예약 요청 → 항공편 확인 → 결제 → 티켓 발급
     └─────────────── 하나의 Trace ───────────────┘
```

### Span (스팬)
Trace 내의 개별 작업 단위. 이름, 시작/종료 시간, 속성, 상태를 포함합니다. Span들은 부모-자식 관계로 연결됩니다.

```
Parent Span: reservation-service: POST /v1/reservations
├── Child Span: flight-service: check-availability
├── Child Span: payment-service: process-payment
└── Child Span: ticket-service: issue-ticket
```

### Trace Context (트레이스 컨텍스트)
서비스 간에 전파되는 트레이스 정보. 주로 HTTP 헤더나 메시지 헤더에 포함됩니다.

```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             │   └──────── trace-id ────────┘  └── span-id ──┘  └ flags
             └── version
```

### Context Propagation (컨텍스트 전파)
서비스 간에 Trace Context를 전달하는 메커니즘. W3C Trace Context 표준을 따릅니다.

### Sampling (샘플링)
모든 트레이스를 저장하지 않고 일부만 선택적으로 저장하는 전략. 운영 환경에서 성능과 비용을 최적화합니다.

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10%만 샘플링
```

---

## OpenTelemetry

### OTel
OpenTelemetry의 약어. 분산 추적, 메트릭, 로그를 위한 표준화된 관찰성(Observability) 프레임워크입니다.

### OTel SDK
OpenTelemetry Software Development Kit. 애플리케이션 내에서 텔레메트리 데이터를 생성하고 처리하는 라이브러리입니다.

### OTel Collector
텔레메트리 데이터를 수집, 처리, 내보내기하는 별도의 프로세스. Receiver, Processor, Exporter로 구성됩니다.

### OTLP (OpenTelemetry Protocol)
OpenTelemetry 데이터를 전송하기 위한 표준 프로토콜. gRPC(4317) 또는 HTTP(4318)를 사용합니다.

### Instrumentation (계측)
코드에서 텔레메트리 데이터를 수집하는 것. 자동 계측(Auto-Instrumentation)과 수동 계측(Manual Instrumentation)이 있습니다.

### Semantic Conventions (시맨틱 컨벤션)
Span 속성에 대한 표준화된 이름 규칙. 예: `http.method`, `messaging.system`, `db.name` 등.

---

## Jaeger

### Jaeger Collector
애플리케이션에서 전송된 Span 데이터를 수신하고 저장소에 쓰는 컴포넌트. CQRS의 Command Side에 해당합니다.

### Jaeger Query
저장소에서 트레이스를 조회하고 Jaeger UI에 제공하는 컴포넌트. CQRS의 Query Side에 해당합니다.

### Jaeger All-in-One
Collector, Query, UI가 하나의 프로세스에서 실행되는 개발용 배포 모드입니다.

### Jaeger Ingester
Kafka에서 Span 데이터를 읽어 저장소에 쓰는 컴포넌트. 대용량 처리 시 버퍼링을 위해 사용됩니다.

### Jaeger UI
웹 기반 트레이스 시각화 도구. http://localhost:16686에서 접근합니다.

---

## Kafka

### Producer (프로듀서)
Kafka 토픽에 메시지를 발행하는 클라이언트입니다.

### Consumer (컨슈머)
Kafka 토픽에서 메시지를 구독하고 처리하는 클라이언트입니다.

### Topic (토픽)
메시지가 저장되는 카테고리. 예: `reservation.created`, `payment.approved`.

### Partition (파티션)
토픽을 분할한 단위. 병렬 처리와 확장성을 위해 사용됩니다.

### Offset (오프셋)
파티션 내 메시지의 순차적 위치. Consumer가 어디까지 읽었는지 추적합니다.

### Consumer Group (컨슈머 그룹)
동일한 토픽을 병렬로 소비하는 Consumer들의 집합입니다.

### Broker (브로커)
Kafka 서버 인스턴스. 메시지를 저장하고 클라이언트 요청을 처리합니다.

---

## Spring Boot & Cloud

### OpenFeign
선언적 HTTP 클라이언트. 인터페이스와 어노테이션만으로 REST 클라이언트를 정의합니다.

```kotlin
@FeignClient(name = "flight-service")
interface FlightClient {
    @GetMapping("/v1/flights/{id}")
    fun getFlightById(@PathVariable id: String): Flight
}
```

### Circuit Breaker (서킷 브레이커)
장애 전파를 방지하는 패턴. 연속적인 실패가 발생하면 호출을 차단하고 빠르게 실패를 반환합니다.

| 상태 | 설명 |
|------|------|
| CLOSED | 정상 상태, 요청 통과 |
| OPEN | 장애 상태, 요청 차단 |
| HALF_OPEN | 복구 테스트 상태 |

### Resilience4j
Java 애플리케이션을 위한 장애 복원력 라이브러리. Circuit Breaker, Retry, Rate Limiter 등을 제공합니다.

### Saga Pattern (사가 패턴)
분산 트랜잭션을 관리하는 패턴. 각 서비스가 로컬 트랜잭션을 수행하고, 실패 시 보상 트랜잭션을 실행합니다.

### Compensation (보상 트랜잭션)
Saga에서 앞선 작업을 취소하기 위해 실행하는 역작업입니다.

```
예약 실패 시 보상:
1. 티켓 취소 → 2. 결제 취소 → 3. 좌석 해제
```

### AOP (Aspect-Oriented Programming)
관점 지향 프로그래밍. 횡단 관심사(로깅, 트레이싱 등)를 비즈니스 로직에서 분리합니다.

### Actuator
Spring Boot의 운영 모니터링 도구. 헬스 체크, 메트릭, 환경 정보 등을 제공합니다.

---

## Elasticsearch

### Index (인덱스)
관련 문서들의 컬렉션. RDBMS의 테이블과 유사합니다.

```
jaeger-prod-jaeger-span-2024-01-01  ← 날짜별 인덱스
jaeger-prod-jaeger-span-000001      ← 롤오버 인덱스
```

### Mapping (매핑)
인덱스의 필드 타입과 저장 방식을 정의하는 스키마입니다.

### Dynamic Mapping (동적 매핑)
첫 번째 문서의 필드 타입에 따라 자동으로 매핑을 생성하는 기능입니다.

### ILM (Index Lifecycle Management)
인덱스의 생명주기(생성, 롤오버, 삭제)를 자동으로 관리하는 Elasticsearch 기능입니다.

| 단계 | 설명 |
|------|------|
| Hot | 활발히 쓰기/읽기 |
| Warm | 읽기만 허용 |
| Cold | 가끔 읽기 |
| Delete | 자동 삭제 |

### Rollover (롤오버)
조건(크기, 문서 수, 시간)에 따라 새 인덱스로 전환하는 기능입니다.

### Alias (별칭)
인덱스에 대한 논리적 이름. 실제 인덱스 변경 없이 애플리케이션에서 일관된 이름을 사용합니다.

```
jaeger-prod-jaeger-span-write → jaeger-prod-jaeger-span-000001 (현재 쓰기)
jaeger-prod-jaeger-span-read  → jaeger-prod-jaeger-span-* (모든 인덱스 검색)
```

---

## 아키텍처 패턴

### MSA (Microservices Architecture)
애플리케이션을 작고 독립적인 서비스들로 분리하는 아키텍처 스타일입니다.

### CQRS (Command Query Responsibility Segregation)
읽기(Query)와 쓰기(Command)를 분리하는 패턴. Jaeger의 Collector/Query 분리가 이에 해당합니다.

### Event-Driven Architecture
이벤트를 중심으로 서비스 간 통신을 수행하는 아키텍처입니다.

### Orchestration (오케스트레이션)
중앙 조정자가 서비스 간 흐름을 제어하는 방식. Reservation Service가 이 역할을 수행합니다.

### Choreography (코레오그래피)
각 서비스가 이벤트에 반응하여 독립적으로 동작하는 방식입니다.

---

## 보안

### TLS (Transport Layer Security)
네트워크 통신을 암호화하는 프로토콜입니다.

### SASL (Simple Authentication and Security Layer)
인증을 위한 프레임워크. Kafka에서 SASL/PLAIN, SASL/SCRAM 등을 지원합니다.

### Bearer Token
API 인증을 위한 토큰. HTTP Authorization 헤더에 포함됩니다.

### Secrets Management
비밀번호, API 키 등 민감한 정보를 안전하게 관리하는 방법입니다.

---

## 약어 모음

| 약어 | 전체 이름 | 설명 |
|------|-----------|------|
| OTLP | OpenTelemetry Protocol | OTel 데이터 전송 프로토콜 |
| ILM | Index Lifecycle Management | ES 인덱스 생명주기 관리 |
| CQRS | Command Query Responsibility Segregation | 읽기/쓰기 분리 패턴 |
| MSA | Microservices Architecture | 마이크로서비스 아키텍처 |
| AOP | Aspect-Oriented Programming | 관점 지향 프로그래밍 |
| TLS | Transport Layer Security | 전송 계층 보안 |
| SASL | Simple Authentication and Security Layer | 인증 프레임워크 |
| PII | Personally Identifiable Information | 개인 식별 정보 |
| SDK | Software Development Kit | 소프트웨어 개발 키트 |
| UI | User Interface | 사용자 인터페이스 |
