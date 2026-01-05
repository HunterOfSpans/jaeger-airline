# Jaeger Airline

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://openjdk.org/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Jaeger](https://img.shields.io/badge/Jaeger-2.14.1-blue.svg)](https://www.jaegertracing.io/)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-2.11.0-blueviolet.svg)](https://opentelemetry.io/)

**MSA 환경에서 분산 추적을 학습하고 검증하기 위한 항공 예약 시스템 테스트베드**

[English](README_EN.md) | 한국어

---

## 프로젝트 소개

Jaeger Airline은 마이크로서비스 아키텍처(MSA)에서 **분산 추적(Distributed Tracing)** 을 실습하고 학습하기 위해 설계된 교육용 프로젝트입니다.

실제 항공 예약 시스템을 모방한 4개의 마이크로서비스가 상호작용하며, **OpenTelemetry**와 **Jaeger**를 통해 전체 요청 흐름을 추적할 수 있습니다. 동기 통신(OpenFeign)과 비동기 통신(Kafka) 모두에서 분산 추적이 어떻게 동작하는지 확인할 수 있습니다.

### 주요 특징

- **완전한 MSA 구현**: 4개의 독립적인 마이크로서비스 (Flight, Payment, Ticket, Reservation)
- **동기/비동기 분산 추적**: OpenFeign 자동 계측 + Kafka 수동 계측 비교 학습
- **장애 복원력 패턴**: Circuit Breaker, Retry, Timeout (Resilience4j)
- **보상 트랜잭션**: Saga 패턴 기반 롤백 처리
- **공통 라이브러리**: Gradle Composite Build로 Kafka 추적 라이브러리 공유
- **One-Click 실행**: Docker Compose로 전체 인프라 즉시 구동

---

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client Request                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Reservation Service (8083)                           │
│                    [Kotlin] Orchestration + Saga Pattern                     │
│                         Circuit Breaker (Resilience4j)                       │
└─────────────────────────────────────────────────────────────────────────────┘
            │                         │                         │
            │ OpenFeign               │ Kafka                   │ OpenFeign
            ▼                         ▼                         ▼
┌───────────────────┐    ┌───────────────────┐    ┌───────────────────────────┐
│  Flight Service   │    │  Kafka Cluster    │    │     Payment Service       │
│      (8080)       │    │    (3 Brokers)    │    │         (8082)            │
│      [Java]       │    │                   │    │        [Kotlin]           │
│ Seat Management   │    │  reservation.*    │    │   Payment Processing      │
└───────────────────┘    │  payment.*        │    └───────────────────────────┘
                         │  ticket.*         │                  │
                         └───────────────────┘                  │ Kafka
                                   │                            ▼
                                   │              ┌───────────────────────────┐
                                   │              │     Ticket Service        │
                                   └──────────────│         (8081)            │
                                                  │        [Kotlin]           │
                                                  │    Ticket Issuance        │
                                                  └───────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Observability Stack                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Jaeger UI  │  │  Collector  │  │Elasticsearch│  │      Kafka UI       │ │
│  │   :16686    │  │   :4318     │  │   :9200     │  │       :8085         │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 서비스 구성

| 서비스 | 포트 | 언어 | 역할 |
|--------|------|------|------|
| **Flight Service** | 8080 | Java | 항공편 조회, 좌석 예약/해제 |
| **Ticket Service** | 8081 | Kotlin | 항공권 발급/취소 |
| **Payment Service** | 8082 | Kotlin | 결제 처리/취소 |
| **Reservation Service** | 8083 | Kotlin | 예약 오케스트레이션, Saga 패턴 |

### 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| **Runtime** | Java | 21 |
| **Language** | Kotlin | 1.9.x |
| **Framework** | Spring Boot | 3.3.5 |
| **Cloud** | Spring Cloud | 2023.0.3 |
| **Tracing** | Jaeger | 2.14.1 |
| **Telemetry** | OpenTelemetry | 2.11.0 |
| **Messaging** | Apache Kafka | 3.x |
| **Storage** | Elasticsearch | 8.13.0 |
| **Build** | Gradle | 8.x |

---

## 빠른 시작

### 사전 요구사항

- Docker & Docker Compose v2+
- Java 21+
- 최소 8GB RAM (권장: 16GB)

### 1. 저장소 클론

```bash
git clone https://github.com/HunterOfSpans/jaeger-airline.git
cd jaeger-airline
```

### 2. 전체 시스템 빌드 및 실행

```bash
# 모든 서비스 빌드 및 Docker 컨테이너 실행
./script/build-and-run.sh
```

### 3. 서비스 상태 확인

```bash
# 모든 서비스 헬스 체크
for port in 8080 8081 8082 8083; do
  echo "localhost:$port - $(curl -s http://localhost:$port/actuator/health | jq -r '.status')"
done
```

### 4. 분산 추적 테스트

```bash
# OpenFeign 동기 호출 추적 테스트
./script/test-feign-tracing.sh

# Kafka 비동기 메시징 추적 테스트
./script/test-kafka-tracing.sh
```

### 5. Jaeger UI에서 추적 확인

1. http://localhost:16686 접속
2. Service: `reservation-service` 선택
3. **Find Traces** 클릭
4. 트레이스 선택하여 전체 호출 흐름 확인

---

## API 엔드포인트

### Reservation Service (8083) - 오케스트레이션

```http
POST /v1/reservations                    # 예약 생성
GET  /v1/reservations/{id}               # 예약 조회
POST /v1/reservations/{id}/cancel        # 예약 취소
```

### 분산 추적 테스트 엔드포인트

```http
# OpenFeign 동기 추적
POST /v1/tracing/feign/simple-flow       # 간단한 호출 체인
POST /v1/tracing/feign/complex-flow      # 복잡한 예약 프로세스
POST /v1/tracing/feign/parallel-calls    # 병렬 호출

# Kafka 비동기 추적
POST /v1/tracing/kafka/simple-events     # 간단한 이벤트 체인
POST /v1/tracing/kafka/complex-events    # 복잡한 이벤트 플로우
POST /v1/tracing/kafka/failure-compensation  # 실패/보상 트랜잭션
```

### Flight Service (8080)

```http
GET  /v1/flights                         # 항공편 검색
GET  /v1/flights/{flightId}              # 항공편 상세
POST /v1/flights/{flightId}/availability # 좌석 확인
POST /v1/flights/{flightId}/reserve      # 좌석 예약
POST /v1/flights/{flightId}/release      # 좌석 해제
```

### Payment Service (8082)

```http
POST /v1/payments                        # 결제 처리
GET  /v1/payments/{paymentId}            # 결제 조회
POST /v1/payments/{paymentId}/cancel     # 결제 취소
```

### Ticket Service (8081)

```http
POST /v1/tickets                         # 항공권 발급
GET  /v1/tickets/{ticketId}              # 항공권 조회
POST /v1/tickets/{ticketId}/cancel       # 항공권 취소
```

---

## 분산 추적

이 프로젝트는 두 가지 방식의 분산 추적을 구현합니다:

### 1. OpenFeign 자동 추적

OpenFeign + Micrometer Tracing을 통해 **자동으로** trace context가 전파됩니다.

```kotlin
@FeignClient(name = "flight-service")
interface FlightClient {
    @PostMapping("/v1/flights/{flightId}/reserve")
    fun reserveSeat(@PathVariable flightId: String): SeatResponse
}
// HTTP 헤더(traceparent, tracestate)가 자동 전파됨
```

### 2. Kafka 수동 추적

Kafka 메시징은 `@KafkaOtelTrace` 어노테이션과 AOP로 **수동 계측**합니다.

```kotlin
@KafkaOtelTrace(spanName = "process-payment-event")
@KafkaListener(topics = ["reservation.requested"])
fun handleReservation(message: String, @Headers headers: MessageHeaders) {
    // AOP가 Kafka 헤더에서 trace context 추출
    // 새로운 span이 기존 trace에 연결됨
}
```

### Kafka 이벤트 체인

```
reservation.requested → seat.reserved → payment.approved → ticket.issued → reservation.completed
```

자세한 내용은 [분산 추적 동작 원리](docs/guides/distributed-tracing-overview.md) 문서를 참고하세요.

---

## 프로젝트 구조

```
jaeger-airline/
├── common/
│   └── kafka-tracing/          # 공통 Kafka 추적 라이브러리
│       ├── annotation/         # @KafkaOtelTrace
│       ├── aspect/             # KafkaTracingAspect (AOP)
│       └── config/             # Spring Boot 자동 구성
├── flight/                     # Flight Service (Java)
├── payment/                    # Payment Service (Kotlin)
├── ticket/                     # Ticket Service (Kotlin)
├── reservation/                # Reservation Service (Kotlin)
├── jaeger/
│   ├── collector/              # Jaeger Collector 설정
│   └── query/                  # Jaeger Query 설정
├── docs/                       # 기술 문서
│   ├── getting-started/        # 시작하기 가이드
│   ├── architecture/           # 아키텍처 설계
│   ├── guides/                 # 실습 가이드
│   ├── reference/              # 기술 참조
│   └── troubleshooting/        # 문제 해결
├── script/                     # 빌드/테스트 스크립트
├── docker-compose.yml          # Jaeger + Elasticsearch
└── docker-compose-kafka.yml    # Kafka 클러스터
```

---

## 문서

| 문서 | 설명 |
|------|------|
| [내 프로젝트에 적용하기](docs/getting-started/apply-to-your-project.md) | **단계별 적용 가이드** (복사해서 사용) |
| [개념 가이드](docs/getting-started/jaeger-otel.md) | Spring Boot + OTel + Jaeger 이론 |
| [분산 추적 동작 원리](docs/guides/distributed-tracing-overview.md) | 전체 추적 동작 원리 설명 |
| [OpenFeign 추적 가이드](docs/guides/OpenFeign-Distributed-Tracing-Guide.md) | 동기 통신 추적 구현 |
| [Kafka 추적 가이드](docs/guides/Kafka-Distributed-Tracing-Complete-Guide.md) | 비동기 메시징 추적 구현 |
| [라이브러리 공유 가이드](docs/architecture/library-sharing-guide.md) | Gradle Composite Build |
| [ES 매핑 충돌 해결](docs/troubleshooting/elasticsearch-mapping-conflict.md) | 트러블슈팅 |

전체 문서 목록은 [docs/README.md](docs/README.md)를 참고하세요.

---

## 모니터링 UI

| 서비스 | URL | 용도 |
|--------|-----|------|
| **Jaeger UI** | http://localhost:16686 | 분산 추적 시각화 |
| **Kafka UI** | http://localhost:8085 | 메시지 큐 모니터링 |
| **Kibana** | http://localhost:5601 | 로그 분석 |

---

## 학습 목표

이 프로젝트를 통해 다음을 학습할 수 있습니다:

1. **분산 추적 시스템** - OpenTelemetry SDK + Jaeger 백엔드 구성
2. **자동 vs 수동 계측** - OpenFeign 자동 추적, Kafka 수동 추적 비교
3. **MSA 통신 패턴** - 동기(REST) / 비동기(메시징) 패턴
4. **장애 복원력** - Circuit Breaker, Retry, Timeout 패턴
5. **보상 트랜잭션** - Saga 패턴으로 분산 트랜잭션 일관성 유지
6. **관찰 가능성** - 분산 시스템 디버깅 및 성능 분석

---

## 스크립트

```bash
./script/build-and-run.sh        # 빌드 및 실행
./script/rebuild-and-restart.sh  # Docker 이미지 재빌드 포함
./script/request.sh              # 기본 예약 플로우 테스트
./script/test-api.sh             # 전체 API 테스트
./script/test-feign-tracing.sh   # OpenFeign 추적 테스트
./script/test-kafka-tracing.sh   # Kafka 추적 테스트
```

---

## 기여하기

이슈와 Pull Request를 환영합니다!

1. 이 저장소를 Fork 합니다
2. 새로운 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add amazing feature'`)
4. 브랜치에 Push 합니다 (`git push origin feature/amazing-feature`)
5. Pull Request를 생성합니다

### 개발 환경 설정

```bash
# 인프라만 실행 (로컬 개발 시)
docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d elasticsearch kafka1 kafka2 kafka3 jaeger-collector jaeger-query

# 특정 서비스만 로컬 실행
cd reservation && ./gradlew bootRun
```

---

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

---

## 감사의 말

- [Jaeger](https://www.jaegertracing.io/) - 분산 추적 백엔드
- [OpenTelemetry](https://opentelemetry.io/) - 관찰 가능성 프레임워크
- [Spring Boot](https://spring.io/projects/spring-boot) - 애플리케이션 프레임워크
- [Resilience4j](https://resilience4j.readme.io/) - 장애 복원력 라이브러리
