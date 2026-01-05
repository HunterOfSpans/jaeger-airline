# 내 프로젝트에 Jaeger + OpenTelemetry 적용하기

> **작성일**: 2026-01-06 | **버전**: Jaeger 2.14.1, Spring Boot 3.3.5, OpenTelemetry 2.11.0 | **난이도**: 입문

이 가이드는 **jaeger-airline 프로젝트를 참고하여** 여러분의 MSA 프로젝트에 분산 추적을 적용하는 단계별 실습 가이드입니다.

---

## 목차

- [사전 준비](#사전-준비)
- [Step 1: 인프라 설정](#step-1-인프라-설정)
- [Step 2: 의존성 추가](#step-2-의존성-추가)
- [Step 3: application.yml 설정](#step-3-applicationyml-설정)
- [Step 4: 서비스 간 통신 설정](#step-4-서비스-간-통신-설정)
- [Step 5: 검증](#step-5-검증)
- [적용 체크리스트](#적용-체크리스트)
- [참고 파일 위치](#참고-파일-위치)

---

## 사전 준비

### 필수 요구사항

- Docker & Docker Compose v2+
- Java 17+ (권장: Java 21)
- Spring Boot 3.x
- Gradle 또는 Maven

### 적용 범위 결정

| 통신 방식 | 계측 방법 | 난이도 | 이 프로젝트 예시 |
|-----------|----------|--------|------------------|
| REST API (컨트롤러) | 자동 | 쉬움 | 모든 서비스 |
| OpenFeign | 자동 | 쉬움 | reservation → flight/payment/ticket |
| RestTemplate/WebClient | 자동 | 쉬움 | - |
| Kafka | **수동** | 중간 | 모든 서비스 (Kafka 리스너) |

---

## Step 1: 인프라 설정

### 1.1 최소 구성 (개발용)

아래 `docker-compose.yml`을 프로젝트 루트에 생성하세요.

```yaml
# docker-compose.yml
services:
  jaeger:
    image: jaegertracing/jaeger:2.14.1
    container_name: jaeger
    ports:
      - "16686:16686"   # Jaeger UI
      - "4317:4317"     # OTLP gRPC
      - "4318:4318"     # OTLP HTTP
    environment:
      - COLLECTOR_OTLP_ENABLED=true
```

```bash
# 실행
docker compose up -d jaeger

# 확인
open http://localhost:16686
```

### 1.2 운영용 구성 (Elasticsearch 저장소)

데이터 영속성이 필요하면 Elasticsearch를 추가합니다.

```yaml
# docker-compose.yml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q 'yellow\\|green'"]
      interval: 10s
      timeout: 5s
      retries: 10

  jaeger:
    image: jaegertracing/jaeger:2.14.1
    container_name: jaeger
    ports:
      - "16686:16686"
      - "4317:4317"
      - "4318:4318"
    environment:
      - SPAN_STORAGE_TYPE=elasticsearch
      - ES_SERVER_URLS=http://elasticsearch:9200
    depends_on:
      elasticsearch:
        condition: service_healthy

volumes:
  es-data:
```

> **참고**: 이 프로젝트의 `docker-compose.yml` 파일 참고

---

## Step 2: 의존성 추가

### 2.1 Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    // 필수: Spring Boot Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // 필수: Micrometer Tracing + OpenTelemetry Bridge
    implementation("io.micrometer:micrometer-tracing-bridge-otel")

    // 필수: OTLP Exporter (Jaeger로 전송)
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // OpenFeign 사용 시: 자동 추적 지원
    implementation("io.github.openfeign:feign-micrometer")
}
```

### 2.2 Gradle (Groovy)

```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp'

    // OpenFeign 사용 시
    implementation 'io.github.openfeign:feign-micrometer'
}
```

### 2.3 Maven

```xml
<!-- pom.xml -->
<dependencies>
    <!-- 필수 -->
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

    <!-- OpenFeign 사용 시 -->
    <dependency>
        <groupId>io.github.openfeign</groupId>
        <artifactId>feign-micrometer</artifactId>
    </dependency>
</dependencies>
```

> **참고**: 이 프로젝트의 `flight/build.gradle.kts` 또는 `reservation/build.gradle.kts` 참고

---

## Step 3: application.yml 설정

### 3.1 기본 설정 (복사해서 사용)

```yaml
# application.yml
spring:
  application:
    name: my-service-name  # 중요! Jaeger에 표시될 서비스 이름

management:
  tracing:
    sampling:
      probability: 1.0  # 개발: 1.0 (100%), 운영: 0.1 (10%) 권장
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces  # Jaeger OTLP HTTP 엔드포인트

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### 3.2 Docker 환경 설정

Docker에서 실행할 때는 `localhost` 대신 컨테이너 이름을 사용합니다.

```yaml
# application-docker.yml
management:
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

### 3.3 환경별 설정 예시

```yaml
# application.yml (공통)
spring:
  application:
    name: order-service

management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLE_RATE:1.0}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

```bash
# 운영 환경 실행 시
TRACING_SAMPLE_RATE=0.1 \
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces \
java -jar myapp.jar
```

> **참고**: 이 프로젝트의 `flight/src/main/resources/application.yml` 참고

---

## Step 4: 서비스 간 통신 설정

### 4.1 REST API (자동 - 설정만 하면 됨)

Spring MVC 컨트롤러는 **자동으로 추적**됩니다. 추가 설정 불필요.

```java
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable String orderId) {
        // 자동으로 Span 생성됨
        return orderService.findById(orderId);
    }
}
```

### 4.2 OpenFeign (자동 - 의존성만 추가)

`feign-micrometer` 의존성을 추가하면 **자동으로 추적**됩니다.

```java
@FeignClient(name = "payment-service", url = "${services.payment.url}")
public interface PaymentClient {

    @PostMapping("/v1/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
    // HTTP 헤더로 trace context 자동 전파
}
```

> **참고**: 이 프로젝트의 `reservation/src/main/kotlin/com/airline/reservation/client/` 참고

### 4.3 Kafka (수동 - 코드 작성 필요)

Kafka는 자동 추적이 안 됩니다. 두 가지 방법이 있습니다:

#### 방법 A: 이 프로젝트의 kafka-tracing 라이브러리 사용

```kotlin
// settings.gradle.kts
includeBuild("path/to/jaeger-airline/common/kafka-tracing")

// build.gradle.kts
dependencies {
    implementation("com.airline:kafka-tracing")
}
```

```kotlin
// Kafka 리스너에 어노테이션 추가
@KafkaOtelTrace(spanName = "process-order-event")
@KafkaListener(topics = ["order.created"])
fun handleOrderCreated(message: String, @Headers headers: MessageHeaders) {
    // 자동으로 trace context 추출 및 span 생성
}
```

#### 방법 B: 직접 구현 (AOP 방식)

```kotlin
// 1. 어노테이션 정의
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KafkaOtelTrace(val spanName: String = "")

// 2. AOP Aspect 구현
@Aspect
@Component
class KafkaTracingAspect(private val tracer: Tracer) {

    private val propagator = W3CTraceContextPropagator.getInstance()

    @Around("@annotation(kafkaOtelTrace)")
    fun traceKafkaMessage(joinPoint: ProceedingJoinPoint, kafkaOtelTrace: KafkaOtelTrace): Any? {
        // 1. Kafka 헤더에서 trace context 추출
        val headers = extractHeaders(joinPoint)
        val parentContext = propagator.extract(Context.current(), headers, HeaderGetter)

        // 2. 새로운 span 생성 (부모 context에 연결)
        val spanName = kafkaOtelTrace.spanName.ifEmpty { joinPoint.signature.name }
        val span = tracer.spanBuilder(spanName)
            .setParent(parentContext)
            .setSpanKind(SpanKind.CONSUMER)
            .startSpan()

        return try {
            span.makeCurrent().use {
                joinPoint.proceed()
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

> **참고**: 이 프로젝트의 `common/kafka-tracing/` 전체 구현 참고

#### Kafka Producer 설정 (trace context 전파)

```kotlin
@Service
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val tracer: Tracer
) {
    private val propagator = W3CTraceContextPropagator.getInstance()

    fun publishOrderCreated(order: Order) {
        val headers = mutableMapOf<String, String>()

        // 현재 trace context를 헤더에 주입
        propagator.inject(Context.current(), headers) { carrier, key, value ->
            carrier?.put(key, value)
        }

        val record = ProducerRecord<String, String>("order.created", order.id, toJson(order))
        headers.forEach { (key, value) ->
            record.headers().add(key, value.toByteArray())
        }

        kafkaTemplate.send(record)
    }
}
```

> **참고**: 이 프로젝트의 `reservation/src/main/kotlin/com/airline/reservation/service/KafkaTracingTestService.kt` 참고

---

## Step 5: 검증

### 5.1 서비스 시작

```bash
# 1. 인프라 시작
docker compose up -d

# 2. 애플리케이션 시작
./gradlew bootRun

# 3. Jaeger UI 접속
open http://localhost:16686
```

### 5.2 테스트 요청 보내기

```bash
# API 호출
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": "PROD-001", "quantity": 1}'

# Jaeger에서 확인
# 1. Service: my-service-name 선택
# 2. Find Traces 클릭
# 3. 트레이스 선택하여 상세 확인
```

### 5.3 서비스 등록 확인

```bash
# Jaeger API로 등록된 서비스 목록 확인
curl -s http://localhost:16686/api/services | jq .

# 예상 결과
{
  "data": [
    "my-service-name",
    "order-service",
    "payment-service"
  ]
}
```

### 5.4 추적 연결 확인

여러 서비스가 연결되어 있는지 확인하려면:

```bash
# 특정 서비스의 트레이스 조회
curl -s "http://localhost:16686/api/traces?service=my-service-name&limit=10" | jq '.data[0].spans | length'

# 여러 서비스가 연결되어 있으면 spans 개수가 2개 이상
```

---

## 적용 체크리스트

### 인프라

- [ ] Docker Compose로 Jaeger 실행
- [ ] Jaeger UI 접속 확인 (http://localhost:16686)
- [ ] OTLP 엔드포인트 접속 가능 확인

### 각 서비스별

- [ ] `spring.application.name` 설정 (고유한 서비스 이름)
- [ ] `spring-boot-starter-actuator` 의존성 추가
- [ ] `micrometer-tracing-bridge-otel` 의존성 추가
- [ ] `opentelemetry-exporter-otlp` 의존성 추가
- [ ] `management.otlp.tracing.endpoint` 설정
- [ ] `management.tracing.sampling.probability` 설정

### 서비스 간 통신

- [ ] OpenFeign 사용 시: `feign-micrometer` 의존성 추가
- [ ] Kafka 사용 시: 수동 계측 구현 또는 kafka-tracing 라이브러리 적용

### 검증

- [ ] 단일 서비스 트레이스 Jaeger에서 확인
- [ ] 서비스 간 호출 시 트레이스 연결 확인
- [ ] Kafka 이벤트 시 트레이스 연결 확인 (해당하는 경우)

---

## 참고 파일 위치

이 프로젝트에서 참고할 수 있는 실제 코드:

| 항목 | 파일 위치 |
|------|----------|
| **Gradle 의존성** | `flight/build.gradle.kts`, `reservation/build.gradle.kts` |
| **application.yml** | `flight/src/main/resources/application.yml` |
| **OpenFeign 클라이언트** | `reservation/src/main/kotlin/com/airline/reservation/client/` |
| **Kafka 추적 라이브러리** | `common/kafka-tracing/` |
| **Kafka 리스너 (수동 계측)** | `payment/src/main/kotlin/com/airline/payment/listener/` |
| **Kafka Producer (context 전파)** | `reservation/src/main/kotlin/com/airline/reservation/service/KafkaTracingTestService.kt` |
| **Docker Compose** | `docker-compose.yml`, `docker-compose-kafka.yml` |

---

## 다음 단계

기본 설정이 완료되면 다음 문서를 참고하세요:

1. [분산 추적 동작 원리](../guides/distributed-tracing-overview.md) - 전체 흐름 이해
2. [OpenFeign 추적 가이드](../guides/OpenFeign-Distributed-Tracing-Guide.md) - 동기 통신 상세
3. [Kafka 추적 가이드](../guides/Kafka-Distributed-Tracing-Complete-Guide.md) - 비동기 통신 상세
4. [ES 매핑 충돌 해결](../troubleshooting/elasticsearch-mapping-conflict.md) - 트러블슈팅

---

## 자주 묻는 질문

### Q: Trace가 Jaeger에 안 보여요

1. `spring.application.name` 설정 확인
2. `management.tracing.sampling.probability=1.0` 확인
3. OTLP 엔드포인트 접근 가능 여부 확인
4. 로그에서 에러 확인: `logging.level.io.opentelemetry=DEBUG`

### Q: 서비스 간 트레이스가 연결이 안 돼요

1. OpenFeign: `feign-micrometer` 의존성 확인
2. Kafka: 수동 계측 코드에서 context 추출/주입 확인
3. 네트워크: 서비스 간 HTTP 헤더가 전달되는지 확인

### Q: 운영 환경에서 샘플링 비율은?

- 트래픽이 많으면 `0.01` ~ `0.1` (1%~10%)
- 트래픽이 적으면 `0.5` ~ `1.0` (50%~100%)
- 에러는 항상 수집하려면 Tail-based sampling 고려

### Q: gRPC vs HTTP 어떤 걸 써야 하나요?

- **HTTP (4318)**: 설정 간단, 방화벽 친화적
- **gRPC (4317)**: 성능 우수, 바이너리 프로토콜
- 일반적으로 HTTP로 시작하고, 성능 이슈 시 gRPC 전환
