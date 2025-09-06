# Jaeger 항공 예약 시스템 | MSA 분산 추적 테스트베드

이 프로젝트는 **Jaeger**를 활용한 분산 추적 검증 시스템으로, 완전한 마이크로서비스 아키텍처(MSA)에서 분산 추적 기능을 시연하고 검증하기 위해 설계되었습니다.

## 🏗️ 아키텍처 개요

### 마이크로서비스 구성
- **Flight Service** (Java/Spring Boot) - 항공편 정보 및 좌석 관리 
- **Payment Service** (Kotlin/Spring Boot) - 결제 처리 및 검증
- **Ticket Service** (Kotlin/Spring Boot) - 항공권 발급 및 관리
- **Reservation Service** (Kotlin/Spring Boot) - 예약 오케스트레이션 및 Saga 패턴 구현

### 핵심 기술 스택
- **Backend**: Java 17+, Kotlin, Spring Boot 3.3.5, Spring Cloud 2023.0.3
- **Service Communication**: OpenFeign (동기), Kafka (비동기)
- **Resilience Patterns**: Resilience4j (Circuit Breaker, Retry, Timeout)
- **Distributed Tracing**: Jaeger + OpenTelemetry
- **Messaging**: Apache Kafka Cluster (3-node)
- **Containerization**: Docker & Docker Compose
- **Build**: Gradle

## 🚀 빠른 시작

### 사전 요구사항
- Docker & Docker Compose
- Java 17+
- Gradle 7+

### 1. 환경 설정
```bash
git clone https://github.com/HunterOfSpans/jaeger-airline.git
cd jaeger-airline
```

### 2. 모든 서비스 시작
```bash
# 기본 빌드 및 실행 (권장)
./build-and-run.sh

# 또는 완전 재빌드 및 재시작 (Docker 이미지까지 재생성)
./rebuild-and-restart.sh
```

### 3. 분산 추적 테스트
```bash
# 기본 예약 플로우 테스트
./request.sh

# OpenFeign 동기 호출 분산 추적 테스트
./test-feign-tracing.sh

# Kafka 비동기 메시징 분산 추적 테스트
./test-kafka-tracing.sh

# 전체 API 엔드포인트 테스트
./test-api.sh
```

### 4. 서비스 접근 포인트
| 서비스 | 포트 | 접속 URL | Health Check |
|--------|------|----------|-------------|
| Flight Service | 8080 | http://localhost:8080 | http://localhost:8080/actuator/health |
| Ticket Service | 8081 | http://localhost:8081 | http://localhost:8081/actuator/health |
| Payment Service | 8082 | http://localhost:8082 | http://localhost:8082/actuator/health |
| Reservation Service | 8083 | http://localhost:8083 | http://localhost:8083/actuator/health |
| **Jaeger UI** | **16686** | **http://localhost:16686** | **분산 추적 시각화** |
| **Kafka UI** | **8085** | **http://localhost:8085** | **메시징 모니터링** |
| Kibana | 5601 | http://localhost:5601 | 로그 분석 |

## 🔄 MSA 패턴 구현

### 1. OpenFeign 기반 서비스 간 통신
```kotlin
@FeignClient(name = "payment-service", url = "\${services.payment.url}")
interface PaymentClient {
    @PostMapping("/v1/payments")
    fun processPayment(@RequestBody request: PaymentRequest): PaymentResponse
}
```

### 2. Circuit Breaker 패턴 (Resilience4j)
```kotlin
@CircuitBreaker(name = "reservation", fallbackMethod = "createReservationFallback")
fun createReservation(request: ReservationRequest): ReservationResponse {
    // 서비스 오케스트레이션 로직
}
```

### 3. Saga 패턴 (보상 트랜잭션)
```kotlin
private fun executeCompensation(reservationId: String, flightId: String) {
    logger.info("Executing compensation for reservation: {}", reservationId)
    // 1. 티켓 취소
    // 2. 결제 취소  
    // 3. 좌석 해제
}
```

## 📋 REST API 엔드포인트

### Flight Service
```http
GET  /v1/flights?from={출발지}&to={도착지}&date={날짜}  # 항공편 검색
GET  /v1/flights/{flightId}                            # 항공편 상세 조회
POST /v1/flights/{flightId}/availability               # 좌석 가용성 확인
POST /v1/flights/{flightId}/reserve                    # 좌석 예약
POST /v1/flights/{flightId}/release                    # 좌석 해제
```

### Reservation Service (오케스트레이션)
```http
POST /v1/reservations                    # 완전한 예약 플로우 실행
GET  /v1/reservations/{reservationId}    # 예약 상태 조회
POST /v1/reservations/{reservationId}/cancel  # 예약 취소
```

### Payment Service
```http
POST /v1/payments                        # 결제 처리
GET  /v1/payments/{paymentId}            # 결제 조회
POST /v1/payments/{paymentId}/cancel     # 결제 취소
```

### Ticket Service
```http
POST /v1/tickets                         # 항공권 발급
GET  /v1/tickets/{ticketId}              # 항공권 조회
POST /v1/tickets/{ticketId}/cancel       # 항공권 취소
```

## 🎯 완전한 예약 플로우 테스트

### 1. 성공 시나리오
```bash
curl -X POST http://localhost:8083/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "flightId": "OZ456",
    "passengerInfo": {
      "name": "김승객",
      "email": "passenger@example.com", 
      "phone": "010-1234-5678"
    },
    "seatPreference": "window",
    "paymentMethod": "CARD"
  }'
```

**예상 응답:**
```json
{
  "reservationId": "RES-12345678",
  "status": "CONFIRMED",
  "flightId": "OZ456",
  "paymentId": "PAY-87654321",
  "ticketId": "TKT-11223344",
  "totalAmount": 850000,
  "seatNumber": "12A",
  "message": "Reservation completed successfully"
}
```

## 🔍 Jaeger 분산 추적 검증

### 1. 추적 데이터 확인
1. **Jaeger UI 접속**: http://localhost:16686
2. **Service 선택**: `reservation-service`
3. **Operation 선택**: `POST /v1/reservations`
4. **Find Traces** 클릭

### 2. 확인 가능한 추적 정보
- **전체 요청 플로우**: Reservation → Flight → Payment → Ticket
- **각 서비스 응답 시간**: 개별 서비스별 성능 분석
- **에러 추적**: 실패 지점 및 스택 트레이스
- **서비스 의존성 맵**: 서비스 간 호출 관계 시각화

### 3. 성능 메트릭 분석
- **Total Duration**: 전체 예약 처리 시간
- **Service Time**: 각 서비스별 처리 시간
- **Network Latency**: 서비스 간 네트워크 지연
- **Database Operations**: 데이터 처리 시간

## 🛡️ 장애 복원력 패턴 검증

### 1. Circuit Breaker 테스트
```bash
# Payment 서비스 중지 후 예약 시도
# 자동으로 Circuit Breaker가 동작하여 빠른 실패 처리
curl -s http://localhost:8083/actuator/circuitbreakers | jq .
```

### 2. 보상 트랜잭션 테스트
```bash
# 결제 실패 시나리오에서 자동 롤백 확인
# 1. 좌석 해제
# 2. 이미 발급된 티켓 취소 
# 3. 데이터 일관성 유지
```

### 3. Kafka 메시징 검증
```bash
# 각 서비스별 이벤트 발송 확인
# - reservation.created (예약 완료)
# - payment.approved (결제 승인)
# - ticket.issued (항공권 발급)
```

## 📊 모니터링 및 관리

### Circuit Breaker 상태 모니터링
```bash
curl http://localhost:8083/actuator/circuitbreakers
```

### 서비스 헬스 체크
```bash
curl http://localhost:8083/actuator/health
```

### Kafka 컨슈머 상태 확인
- Kafka UI: http://localhost:8085
- Topics: `reservation.created`, `payment.approved`, `ticket.issued`

## 🧪 분산 추적 테스트 시나리오

### 1. 통합 테스트 스크립트

#### `./test-feign-tracing.sh` - OpenFeign 동기 호출 추적
```bash
# 실행 후 Jaeger UI에서 확인 가능한 추적:
# - 간단한 동기 호출 체인
# - 복잡한 예약 프로세스 (Circuit Breaker 포함)
# - Circuit Breaker 동작 테스트
# - 병렬 호출 테스트
```

#### `./test-kafka-tracing.sh` - Kafka 비동기 메시징 추적
```bash
# 실행 후 확인 가능한 추적:
# - 간단한 이벤트 체인 (reservation → payment → ticket)
# - 복잡한 이벤트 플로우
# - 실패 및 보상 트랜잭션
# - 다중 토픽 이벤트 처리
```

#### `./test-api.sh` - 전체 API 엔드포인트 테스트
```bash
# 모든 서비스의 API 엔드포인트 검증:
# - 각 서비스별 헬스 체크
# - CRUD 작업 테스트
# - 에러 핸들링 검증
```

### 2. 실시간 분산 추적 확인 방법

1. **테스트 실행**:
   ```bash
   ./test-feign-tracing.sh  # 또는 다른 테스트 스크립트
   ```

2. **Jaeger UI 접속**: http://localhost:16686

3. **추적 검색**:
   - Service: `reservation-service` 선택
   - Operation: 원하는 작업 선택 (예: `POST /v1/tracing/feign/complex-flow`)
   - Find Traces 클릭

4. **분석 가능한 정보**:
   - 🕒 **Timeline View**: 서비스 간 호출 순서와 시간
   - 📊 **Span Details**: 각 단계별 상세 정보
   - ❗ **Error Tracking**: 실패 지점과 스택 트레이스
   - 🔗 **Service Map**: 서비스 의존성 관계

### 3. 장애 복원력 패턴 검증

#### Circuit Breaker 동작 확인
```bash
# Circuit Breaker 상태 모니터링
curl -s http://localhost:8083/actuator/circuitbreakers | jq .

# 실행 후 Jaeger에서 확인:
# - CLOSED → OPEN 상태 전환
# - Fallback 메소드 실행 추적
# - 자동 복구 (HALF_OPEN) 과정
```

#### Kafka 이벤트 체인 검증
```bash
# Kafka UI에서 메시지 확인: http://localhost:8085
# Topics:
# - reservation.requested  (예약 요청)
# - seat.reserved         (좌석 예약 완료)
# - payment.approved      (결제 승인)
# - ticket.issued         (항공권 발급)
```

## 📁 프로젝트 구조

```
jaeger-airline/
├── flight/                     # Flight Service (Java 17)
│   ├── src/main/java/
│   │   ├── controller/         # REST API 컨트롤러
│   │   ├── service/           # 비즈니스 로직
│   │   ├── aspect/            # 분산 추적 AOP
│   │   ├── listener/          # Kafka 리스너
│   │   └── dto/               # 데이터 전송 객체
│   └── src/main/resources/
├── payment/                    # Payment Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── controller/        # REST API 컨트롤러
│   │   ├── service/          # 결제 비즈니스 로직
│   │   ├── aspect/           # Kafka 분산 추적 AOP
│   │   ├── listener/         # Kafka 이벤트 리스너
│   │   └── config/           # 설정 (Kafka, OpenTelemetry)
├── ticket/                     # Ticket Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── controller/       # 항공권 API
│   │   ├── service/          # 항공권 발급 로직
│   │   ├── aspect/           # 분산 추적 AOP
│   │   ├── listener/         # Kafka 리스너
│   │   └── annotation/       # @KafkaOtelTrace 커스텀 어노테이션
├── reservation/                # Reservation Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── controller/       # Reservation API + 분산 추적 테스트 API
│   │   ├── service/          # 오케스트레이션 + Kafka 이벤트 발행
│   │   ├── client/           # OpenFeign 클라이언트
│   │   ├── aspect/           # 분산 추적 AOP
│   │   ├── listener/         # Kafka 리스너
│   │   └── config/           # Circuit Breaker + OpenTelemetry 설정
│   └── src/main/resources/
│       └── application-circuit.yml # Circuit Breaker 상세 설정
├── 📋 인프라 설정
├── docker-compose-kafka.yml   # Kafka 3-node 클러스터
├── docker-compose.yml         # Jaeger + Elasticsearch + Kibana
├── 📋 실행 스크립트
├── build-and-run.sh           # 기본 빌드 및 실행
├── rebuild-and-restart.sh     # 완전 재빌드 및 재시작
├── 📋 테스트 스크립트
├── request.sh                 # 기본 예약 플로우 테스트
├── test-api.sh               # 전체 API 엔드포인트 테스트
├── test-feign-tracing.sh     # OpenFeign 분산 추적 테스트
├── test-kafka-tracing.sh     # Kafka 분산 추적 테스트
├── 📋 분산 추적 문서
├── OpenFeign-Distributed-Tracing-Guide.md    # OpenFeign 자동 추적 가이드
├── Kafka-Distributed-Tracing-Complete-Guide.md # Kafka 수동 추적 완전 가이드
├── Jaeger-CQRS-Architecture-Guide.md         # Jaeger CQRS 아키텍처 분석
└── README.md                  # 이 파일
```

## 📚 분산 추적 심화 학습

### 구현된 분산 추적 패턴

1. **OpenFeign 자동 추적**:
   - `feign-micrometer` 의존성으로 완전 자동화
   - W3C Trace Context 표준 준수
   - HTTP 헤더 자동 전파 (`traceparent`, `tracestate`)
   - 👉 자세한 내용: [OpenFeign-Distributed-Tracing-Guide.md](OpenFeign-Distributed-Tracing-Guide.md)

2. **Kafka 수동 추적**:
   - `@KafkaOtelTrace` 커스텀 어노테이션
   - AOP 기반 trace context 추출/전파
   - MessageHeaders 우선 + ConsumerRecord 호환성 지원
   - 👉 자세한 내용: [Kafka-Distributed-Tracing-Complete-Guide.md](Kafka-Distributed-Tracing-Complete-Guide.md)

3. **Jaeger CQRS 아키텍처**:
   - Collector (Write Side) / Query (Read Side) 분리
   - Elasticsearch 기반 강력한 검색 능력
   - 독립적 스케일링 및 성능 최적화
   - 👉 자세한 내용: [Jaeger-CQRS-Architecture-Guide.md](Jaeger-CQRS-Architecture-Guide.md)

### 🎓 학습 목표

이 프로젝트를 통해 다음을 학습할 수 있습니다:

1. **분산 추적 시스템** - Jaeger + OpenTelemetry
2. **자동 vs 수동 계측** - OpenFeign 자동, Kafka 수동 구현 비교
3. **마이크로서비스 통신** - 동기/비동기 패턴별 추적 전략
4. **장애 복원력 패턴** - Circuit Breaker, Saga Pattern
5. **서비스 오케스트레이션** - 복잡한 비즈니스 플로우 관리
6. **관찰 가능성(Observability)** - 분산 시스템 디버깅 및 모니터링
7. **CQRS 패턴** - Read/Write 분리 아키텍처

## 🛠️ 확장 가능한 아키텍처

### 현재 구현된 패턴
- ✅ **분산 추적** (OpenTelemetry + Jaeger)
- ✅ **Circuit Breaker** (Resilience4j)
- ✅ **Saga Pattern** (보상 트랜잭션)
- ✅ **Event-Driven Architecture** (Kafka)
- ✅ **CQRS-like Pattern** (Jaeger Collector/Query)

### 향후 확장 아이디어
- [ ] **Distributed Caching** (Redis)
- [ ] **API Gateway** (Spring Cloud Gateway)
- [ ] **Service Mesh** (Istio)
- [ ] **Event Sourcing** 패턴
- [ ] **Full CQRS** 패턴 (읽기/쓰기 DB 분리)

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
