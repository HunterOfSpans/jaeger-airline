# Spring Boot 3 + OpenTelemetry + Jaeger v2 개발 가이드

> **작성일**: 2026-01-05 | **버전**: Jaeger 2.10.0, Spring Boot 3.3.5, OpenTelemetry 2.11.0, Java 21 | **난이도**: 입문

---

## 목차

- [1. 핵심 개념 정리](#1-핵심-개념-정리)
- [2. 배포 아키텍처](#2-배포-아키텍처)
- [3. Spring Boot 3 연동](#3-spring-boot-3-연동)
- [4. Jaeger v2 배포](#4-jaeger-v2-배포)
- [5. 커스텀 Span 추가](#5-커스텀-span-추가)
- [6. 주요 포트 정리](#6-주요-포트-정리)
- [7. 트러블슈팅](#7-트러블슈팅)
- [8. 참고 자료](#8-참고-자료)

---

## 1. 핵심 개념 정리

### 1.1 OpenTelemetry 구성요소

| 구성요소 | 위치 | 역할 |
|---------|------|------|
| **OTel API** | 앱 내부 (라이브러리) | 텔레메트리 기록을 위한 인터페이스 정의 |
| **OTel SDK** | 앱 내부 (라이브러리) | API의 구현체. Span 생성, 처리, 내보내기 담당 |
| **OTel Collector** | 별도 프로세스 | 텔레메트리 데이터 수집/가공/라우팅 (선택사항) |

**참고**: SDK는 앱 안에서 동작하는 라이브러리이고, Collector는 별도로 띄우는 인프라 컴포넌트임.

### 1.2 Jaeger v2 아키텍처

Jaeger v2는 **OpenTelemetry Collector 프레임워크 위에 구축**됨.

```
Jaeger v2 = OTel Collector 코어 + Jaeger 전용 컴포넌트
```

**Jaeger v2에 포함된 컴포넌트**:
- **OTel 공식**: OTLP Receiver, Batch Processor, Attribute Processor
- **OTel Contrib**: Kafka Exporter/Receiver, Tail Sampling Processor
- **Jaeger 전용**: Jaeger Storage Extension, Jaeger Query Extension

**결론**: Jaeger v2 자체가 OTel Collector 기반이므로, 별도 OTel Collector 없이 OTLP를 네이티브로 수신 가능.

---

## 2. 배포 아키텍처

### 2.1 옵션 A: Direct to Storage (권장 - 단순 구성)

```
┌─────────────────┐      OTLP       ┌─────────────────┐           ┌──────────────┐
│  Spring Boot    │ ──────────────> │  Jaeger v2      │ ────────> │ Elasticsearch│
│  + OTel SDK     │   (4317/4318)   │  (Collector)    │           │ / Cassandra  │
└─────────────────┘                 └─────────────────┘           └──────────────┘
```

- 가장 단순한 구성
- Jaeger만 사용할 때 적합
- Storage가 피크 트래픽을 처리할 수 있어야 함

### 2.2 옵션 B: Via Kafka (대용량 트래픽)

```
┌─────────────┐     ┌─────────────┐     ┌─────────┐     ┌──────────┐     ┌────────┐
│ Spring Boot │ ──> │ Jaeger      │ ──> │  Kafka  │ ──> │ Ingester │ ──> │   ES   │
│ + OTel SDK  │     │ Collector   │     │         │     │          │     │        │
└─────────────┘     └─────────────┘     └─────────┘     └──────────┘     └────────┘
```

- 데이터 손실 방지
- Kafka가 버퍼 역할
- 대용량 트래픽 환경에 적합

### 2.3 옵션 C: OTel Collector 추가 (여러 백엔드)

```
┌─────────────┐     ┌───────────────┐     ┌─────────────┐     ┌────────┐
│ Spring Boot │ ──> │ OTel          │ ──> │ Jaeger v2   │ ──> │   ES   │
│ + OTel SDK  │     │ Collector     │     └─────────────┘     └────────┘
└─────────────┘     │               │ ──> ┌─────────────┐
                    │               │     │ Prometheus  │
                    │               │     └─────────────┘
                    │               │ ──> ┌─────────────┐
                    └───────────────┘     │ Datadog 등  │
                                          └─────────────┘
```

- 여러 백엔드로 동시 전송 필요시
- Metrics, Logs도 함께 처리시
- 앱 변경 없이 백엔드 교체 가능

---

## 3. Spring Boot 3 연동

### 3.1 방식 1: Micrometer Tracing + OTel Bridge (Spring 권장)

Spring Boot 3에서 Spring Cloud Sleuth는 deprecated되고, **Micrometer Tracing**으로 통합됨.

#### 의존성 (Gradle Kotlin DSL)

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
}
```

#### 의존성 (Maven)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
</dependencies>
```

#### application.yml

```yaml
spring:
  application:
    name: my-service

management:
  tracing:
    sampling:
      probability: 1.0  # 개발: 1.0, 운영: 0.1 등 조절
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces  # HTTP
      # endpoint: http://jaeger:4317  # gRPC 사용시
```

### 3.2 방식 2: OTel Java Agent (Zero-code)

바이트코드 조작으로 자동 계측. 코드 수정 없이 적용 가능.

#### 실행 방법

```bash
# Agent 다운로드
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# 실행
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=my-service \
  -Dotel.exporter.otlp.endpoint=http://jaeger:4317 \
  -Dotel.exporter.otlp.protocol=grpc \
  -jar myapp.jar
```

#### 환경변수 방식

```bash
export OTEL_SERVICE_NAME=my-service
export OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_TRACES_EXPORTER=otlp

java -javaagent:opentelemetry-javaagent.jar -jar myapp.jar
```

### 3.3 방식 비교

| | Micrometer Tracing | OTel Java Agent |
|---|-------------------|-----------------|
| 적용 방식 | 의존성 추가 | JVM 옵션 추가 |
| 코드 수정 | 불필요 (자동 계측) | 불필요 |
| GraalVM Native | ✅ 지원 | ❌ 미지원 |
| 커스텀 Span | `@Observed` 어노테이션 | `@WithSpan` 어노테이션 |
| 계측 범위 | Spring 생태계 중심 | 광범위한 라이브러리 |

---

## 4. Jaeger v2 배포

### 4.1 Docker (개발용 - In-Memory)

```bash
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/jaeger:2.10.0
```

**포트 설명**:
- `16686`: Jaeger UI
- `4317`: OTLP gRPC
- `4318`: OTLP HTTP

### 4.2 Docker Compose (개발용 - Elasticsearch)

```yaml
services:
  jaeger:
    image: jaegertracing/jaeger:2.10.0
    ports:
      - "16686:16686"
      - "4317:4317"
      - "4318:4318"
    environment:
      - SPAN_STORAGE_TYPE=elasticsearch
      - ES_SERVER_URLS=http://elasticsearch:9200
    depends_on:
      - elasticsearch

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

volumes:
  es-data:
```

### 4.3 Kubernetes (OpenTelemetry Operator 사용)

```yaml
# 1. cert-manager 설치
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.1/cert-manager.yaml

# 2. OpenTelemetry Operator 설치
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
```

```yaml
# 3. Jaeger 배포 (OpenTelemetryCollector CRD 사용)
apiVersion: opentelemetry.io/v1beta1
kind: OpenTelemetryCollector
metadata:
  name: jaeger
  namespace: observability
spec:
  image: jaegertracing/jaeger:latest
  ports:
    - name: jaeger-ui
      port: 16686
  config:
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
          http:
            endpoint: 0.0.0.0:4318

    exporters:
      jaeger_storage_exporter:
        trace_storage: memstore

    extensions:
      jaeger_storage:
        backends:
          memstore:
            memory:
              max_traces: 100000
      jaeger_query:
        storage:
          traces: memstore

    service:
      extensions: [jaeger_storage, jaeger_query]
      pipelines:
        traces:
          receivers: [otlp]
          exporters: [jaeger_storage_exporter]
```

---

## 5. 커스텀 Span 추가

### 5.1 Micrometer Tracing 방식

```kotlin
import io.micrometer.observation.annotation.Observed

@Service
class OrderService {

    @Observed(name = "order.process", contextualName = "process-order")
    fun processOrder(orderId: String): Order {
        // 자동으로 Span 생성됨
        return orderRepository.findById(orderId)
    }
}
```

**설정 필요**:
```kotlin
@Configuration
class ObservationConfig {
    @Bean
    fun observedAspect(registry: ObservationRegistry) = ObservedAspect(registry)
}
```

### 5.2 OTel Java Agent 방식

```kotlin
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.opentelemetry.instrumentation.annotations.SpanAttribute

@Service
class OrderService {

    @WithSpan("process-order")
    fun processOrder(
        @SpanAttribute("order.id") orderId: String
    ): Order {
        return orderRepository.findById(orderId)
    }
}
```

### 5.3 수동 Span 생성

```kotlin
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.Span

@Service
class OrderService(private val tracer: Tracer) {

    fun processOrder(orderId: String): Order {
        val span = tracer.spanBuilder("process-order")
            .setAttribute("order.id", orderId)
            .startSpan()

        try {
            span.makeCurrent().use {
                // 비즈니스 로직
                return orderRepository.findById(orderId)
            }
        } catch (e: Exception) {
            span.recordException(e)
            throw e
        } finally {
            span.end()
        }
    }
}
```

---

## 6. 주요 포트 정리

| 포트 | 프로토콜 | 용도 |
|------|----------|------|
| 4317 | gRPC | OTLP gRPC Receiver |
| 4318 | HTTP | OTLP HTTP Receiver |
| 16686 | HTTP | Jaeger UI |
| 14250 | gRPC | Jaeger gRPC (레거시) |
| 14268 | HTTP | Jaeger HTTP (레거시) |
| 9411 | HTTP | Zipkin 호환 |

---

## 7. 트러블슈팅

### 7.1 Trace가 보이지 않을 때

1. **샘플링 확인**: `management.tracing.sampling.probability=1.0` 설정
2. **엔드포인트 확인**: Jaeger가 정상 기동 중인지, 포트가 맞는지 확인
3. **서비스 이름 확인**: `spring.application.name` 설정 필수
4. **네트워크 확인**: 앱 → Jaeger 간 통신 가능 여부

### 7.2 로그 활성화

```yaml
logging:
  level:
    io.opentelemetry: DEBUG
    io.micrometer.tracing: DEBUG
```

### 7.3 헬스체크

```bash
# Jaeger 상태 확인
curl http://localhost:16686/api/services

# OTLP 엔드포인트 확인
curl -X POST http://localhost:4318/v1/traces \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

## 8. 참고 자료

- [Jaeger 공식 문서](https://www.jaegertracing.io/docs/latest/)
- [OpenTelemetry Java 문서](https://opentelemetry.io/docs/languages/java/)
- [Spring Boot Observability](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)

---

## 버전 EOL 정보

| 컴포넌트 | 버전 | EOL |
|---------|------|-----|
| Jaeger v1 | 1.x | 2025-12-31 (지원 종료 예정) |
| Jaeger v2 | 2.10.0+ | - (현재 권장) |