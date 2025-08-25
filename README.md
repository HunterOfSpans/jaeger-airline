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

### 2. Kafka 클러스터 시작
```bash
docker-compose -f docker-compose-kafka.yml up -d
```

### 3. 서비스 빌드 및 실행
```bash
# 각 서비스를 별도 터미널에서 실행
cd flight && ./gradlew bootRun --args='--spring.profiles.active=local'
cd payment && ./gradlew bootRun --args='--spring.profiles.active=local'
cd ticket && ./gradlew bootRun --args='--spring.profiles.active=local'
cd reservation && ./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. 서비스 접근 포인트
| 서비스 | 포트 | Health Check |
|--------|------|-------------|
| Flight Service | 8080 | http://localhost:8080/actuator/health |
| Ticket Service | 8081 | http://localhost:8081/actuator/health |
| Payment Service | 8082 | http://localhost:8082/actuator/health |
| Reservation Service | 8083 | http://localhost:8083/actuator/health |
| Jaeger UI | 16686 | http://localhost:16686 |
| Kafka UI | 8085 | http://localhost:8085 |

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

## 🧪 테스트 시나리오

### 1. 정상 플로우 테스트
- ✅ 항공편 조회 → 좌석 예약 → 결제 → 항공권 발급

### 2. 장애 시나리오 테스트
- ❌ Payment 서비스 다운 → Circuit Breaker 동작
- ❌ 결제 실패 → 보상 트랜잭션 실행
- ❌ 네트워크 지연 → Timeout 처리

### 3. 동시성 테스트
- 🔄 다중 예약 요청 동시 처리
- 🔄 좌석 경합 상황 처리

## 📁 프로젝트 구조

```
jaeger-airline/
├── flight/                     # Flight Service (Java)
│   ├── src/main/java/
│   │   ├── controller/         # REST API 컨트롤러
│   │   ├── service/           # 비즈니스 로직
│   │   └── dto/               # 데이터 전송 객체
│   └── src/main/resources/
│       ├── application.yml    # 기본 설정
│       └── application-local.yml # 로컬 환경 설정
├── payment/                    # Payment Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── controller/        # REST API 컨트롤러
│   │   ├── service/          # 결제 비즈니스 로직
│   │   ├── dto/              # 결제 관련 DTO
│   │   └── config/           # Kafka Producer 설정
│   └── src/main/resources/
├── ticket/                     # Ticket Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── controller/       # 항공권 API
│   │   ├── service/         # 항공권 발급 로직  
│   │   └── dto/             # 항공권 DTO
│   └── src/main/resources/
├── reservation/               # Reservation Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── api/             # Reservation API
│   │   ├── service/         # 오케스트레이션 로직
│   │   ├── client/          # OpenFeign 클라이언트
│   │   ├── dto/             # 예약 관련 DTO
│   │   └── config/          # Circuit Breaker 설정
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── application-circuit.yml # Circuit Breaker 설정
├── docker-compose-kafka.yml   # Kafka 클러스터 설정
├── docker-compose.yml         # Jaeger 및 전체 인프라
└── README.md
```

## 🎓 학습 목표

이 프로젝트를 통해 다음을 학습할 수 있습니다:

1. **분산 추적 시스템** - Jaeger + OpenTelemetry
2. **마이크로서비스 통신** - OpenFeign, Kafka
3. **장애 복원력 패턴** - Circuit Breaker, Saga Pattern
4. **서비스 오케스트레이션** - 복잡한 비즈니스 플로우 관리
5. **모니터링 및 관찰 가능성** - 분산 시스템 디버깅

## 🤝 기여하기

이 프로젝트는 MSA와 분산 추적 학습을 위한 교육용 프로젝트입니다. 

### 개선 아이디어
- [ ] Distributed Caching (Redis)
- [ ] API Gateway (Spring Cloud Gateway)
- [ ] Service Mesh (Istio)
- [ ] Event Sourcing 패턴
- [ ] CQRS 패턴

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

---

📚 **더 자세한 내용은 [English Documentation](README_EN.md)을 참고하세요.**