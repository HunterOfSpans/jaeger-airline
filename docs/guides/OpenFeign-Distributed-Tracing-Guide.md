# OpenFeign 분산 추적 동작원리 상세 가이드

> **작성일**: 2026-01-04 | **버전**: Spring Boot 3.3.5, feign-micrometer 13.6 | **난이도**: 중급

---

## 목차

- [개요](#개요)
- [핵심 아키텍처](#핵심-아키텍처)
- [트레이스 헤더 자동 전파 메커니즘](#트레이스-헤더-자동-전파-메커니즘)
- [실제 구현 분석](#실제-구현-분석)
- [환경변수 및 설정](#환경변수-및-설정)
- [트러블슈팅 가이드](#트러블슈팅-가이드)
- [성능 고려사항](#성능-고려사항)
- [모니터링 및 검증](#모니터링-및-검증)
- [결론](#결론)
- [참고 문헌](#참고-문헌)

---

## 개요

이 문서는 Jaeger 2.6.0 환경에서 Spring Boot 3.x와 OpenFeign이 어떻게 자동으로 분산 추적을 수행하는지에 대한 상세한 기술적 분석을 제공합니다. OpenFeign의 트레이스 헤더 자동 삽입/추출 메커니즘과 OpenTelemetry와의 통합 방식을 심층적으로 다룹니다.

## 핵심 아키텍처

### 1. 자동 계측(Auto-Instrumentation) 메커니즘

OpenFeign에서 분산 추적이 자동으로 작동하는 핵심은 다음 컴포넌트들의 조합입니다:

```
Spring Boot 3.x Application
├── OpenTelemetry Auto-Instrumentation
├── Micrometer Tracing Bridge
├── feign-micrometer 라이브러리 
└── MicrometerObservationCapability (자동 구성)
```

### 2. 필수 의존성

```gradle
dependencies {
    // Spring Cloud OpenFeign
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
    
    // ⭐ 핵심: feign-micrometer (트레이스 헤더 자동 전파)
    implementation 'io.github.openfeign:feign-micrometer:13.6'
    
    // OpenTelemetry 통합
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter'
}
```

**중요**: `feign-micrometer` 의존성이 없으면 트레이스 헤더가 자동으로 전파되지 않습니다.

## 트레이스 헤더 자동 전파 메커니즘

### 1. W3C Trace Context 표준 헤더

OpenFeign은 W3C Trace Context 사양에 따라 다음 HTTP 헤더를 자동으로 처리합니다:

#### traceparent 헤더
```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
```

구조:
- **version** (00): Trace Context 버전
- **trace-id** (32글자): 전체 트레이스 식별자
- **parent-id** (16글자): 부모 스팬 식별자  
- **trace-flags** (02글자): 샘플링 및 트레이스 플래그

#### tracestate 헤더
```
tracestate: vendor1=value1,vendor2=value2
```
벤더별 추가 컨텍스트 정보를 포함합니다.

### 2. 자동 헤더 삽입 과정

```java
// OpenFeign 클라이언트 호출 시 자동으로 발생하는 과정:

@FeignClient(name = "flight-service", url = "${services.flight.url}")
interface FlightClient {
    @GetMapping("/v1/flights/{flightId}")
    fun getFlightById(@PathVariable flightId: String): FlightDto?
}

// 1. MicrometerObservationCapability가 자동으로 활성화됨
// 2. 현재 활성화된 Span에서 Trace Context 추출
// 3. W3C 표준에 따라 traceparent/tracestate 헤더 생성
// 4. HTTP 요청에 헤더 자동 삽입
// 5. 대상 서비스로 전송
```

### 3. 헤더 추출 과정

수신 측 서비스에서는 OpenTelemetry의 자동 계측이 다음과 같이 동작합니다:

```java
// HTTP 요청 수신 시:
// 1. OpenTelemetry HTTP Instrumentation이 traceparent 헤더 감지
// 2. Trace Context 파싱 및 검증
// 3. 새로운 Child Span 생성 (부모 컨텍스트 연결)
// 4. 현재 스레드에 Span Context 설정
```

## 실제 구현 분석

### 1. MicrometerObservationCapability 자동 구성

Spring Boot 3.x는 다음 조건이 충족되면 자동으로 OpenFeign에 관찰 기능을 활성화합니다:

```java
// 자동 구성 조건:
// 1. feign-micrometer가 클래스패스에 존재
// 2. ObservationRegistry Bean이 사용 가능
// 3. @EnableFeignClients 어노테이션 활성화

@SpringBootApplication
@EnableFeignClients  // ← 이 어노테이션으로 자동 구성 트리거
class ReservationApplication
```

### 2. 트레이스 컨텍스트 전파 플로우

```
[Reservation Service]
       ↓ (OpenFeign 호출)
   traceparent: 00-{trace-id}-{parent-span-id}-01
       ↓
[Flight Service] 
   ↓ (새로운 child span 생성)
   traceparent: 00-{same-trace-id}-{new-span-id}-01
       ↓
[응답 반환 + 스팬 종료]
```

### 3. 현재 프로젝트의 실제 구성

#### build.gradle.kts (Reservation Service)
```kotlin
dependencies {
    // OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    
    // Micrometer + OpenTelemetry 통합
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.github.openfeign:feign-micrometer:13.6")  // ← 핵심
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
}
```

#### OpenFeign 클라이언트 구현
```kotlin
@FeignClient(name = "flight-service", url = "\${services.flight.url:http://flight:8080}")
interface FlightClient {
    @GetMapping("/v1/flights/{flightId}")
    fun getFlightById(@PathVariable flightId: String): FlightDto?
}
```

**중요**: 별도의 인터셉터나 헤더 설정 코드 없이 자동으로 트레이스 전파가 됩니다.

## 환경변수 및 설정

### OpenTelemetry 환경변수
```bash
# 서비스 식별
OTEL_SERVICE_NAME=reservation-service

# Jaeger Exporter 설정
OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317
OTEL_TRACES_EXPORTER=otlp

# 샘플링 설정
OTEL_TRACES_SAMPLER=always_on

# HTTP/Spring Web 자동 계측 활성화
OTEL_INSTRUMENTATION_HTTP_ENABLED=true
OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED=true
```

### Docker Compose 설정 예시
```yaml
services:
  reservation:
    environment:
      - OTEL_SERVICE_NAME=reservation-service
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317
      - OTEL_TRACES_EXPORTER=otlp
      - OTEL_TRACES_SAMPLER=always_on
```

## 트러블슈팅 가이드

### 1. 트레이스가 연결되지 않는 경우

**문제**: OpenFeign 호출 시 새로운 트레이스가 생성됨
**원인**: `feign-micrometer` 의존성 누락
**해결**: 
```gradle
implementation 'io.github.openfeign:feign-micrometer:13.6'
```

### 2. 헤더가 전파되지 않는 경우

**문제**: traceparent 헤더가 HTTP 요청에 포함되지 않음
**원인**: 
- MicrometerObservationCapability 자동 구성 실패
- ObservationRegistry Bean 누락

**해결**:
```kotlin
// 필요시 수동 구성
@Configuration
class FeignConfig {
    @Bean
    fun micrometerCapability(observationRegistry: ObservationRegistry): MicrometerObservationCapability {
        return MicrometerObservationCapability(observationRegistry)
    }
}
```

### 3. Spring Boot 3.0.1 호환성 이슈

일부 Spring Boot 3.0.1 버전에서 알려진 문제가 있습니다:
- 증상: Feign이 Trace/Span ID를 전송하지 않음
- 해결: Spring Boot 3.0.2+ 업그레이드 또는 feign-micrometer 명시적 추가

## 성능 고려사항

### 1. 샘플링 전략
```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% 샘플링
```

### 2. 비동기 처리시 주의사항
```kotlin
// 비동기 컨텍스트에서는 수동으로 전파 필요
@Async
fun asyncOperation() {
    // Tracing context가 자동으로 전파되지 않을 수 있음
    // Context Propagation 라이브러리 활용 권장
}
```

## 모니터링 및 검증

### 1. Jaeger UI에서 확인
- URL: http://localhost:16686
- Service: reservation-service 선택
- Operation: HTTP GET/POST 메서드 확인
- Traces: 서비스 간 호출 체인 시각화

### 2. 로그 확인
```bash
# 트레이스 컨텍스트 로그 확인
docker logs reservation | grep -i trace
docker logs flight | grep -i trace
```

### 3. 실제 헤더 확인 (디버깅용)
```kotlin
@Component
class HeaderLoggingInterceptor : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        template.headers().forEach { (key, value) ->
            if (key.lowercase().contains("trace")) {
                log.info("Trace Header - $key: $value")
            }
        }
    }
}
```

## 결론

OpenFeign과 OpenTelemetry의 분산 추적은 다음과 같은 **자동화된 메커니즘**으로 작동합니다:

1. **feign-micrometer** 의존성이 핵심 역할
2. **MicrometerObservationCapability**가 자동 구성됨
3. **W3C Trace Context** 표준 준수
4. **별도 코드 수정 없이** 헤더 자동 전파
5. **OpenTelemetry Auto-Instrumentation**으로 컨텍스트 연결

이러한 자동화 덕분에 개발자는 분산 추적을 위한 복잡한 헤더 조작이나 인터셉터 구현 없이도 마이크로서비스 간 트레이스 연결을 달성할 수 있습니다.

## 참고 문헌

- [Spring Cloud OpenFeign 공식 문서](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [W3C Trace Context 명세](https://www.w3.org/TR/trace-context/)
- [OpenTelemetry Spring Boot Starter](https://opentelemetry.io/blog/2024/spring-starter-stable/)
- [Micrometer Tracing 가이드](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)