# Jaeger Airline Testbed | Jaeger 항공 예약 테스트베드

---

## 한국어

### 개요
이 프로젝트는 **Jaeger**를 활용한 분산 추적 테스트베드로, 마이크로서비스 아키텍처에서 분산 추적 기능을 시연하고 검증하기 위해 설계되었습니다. 비즈니스 도메인은 여러 서비스가 상호 연결된 항공 예약 서비스입니다.

### 아키텍처
테스트베드는 다음 마이크로서비스들로 구성됩니다:
- **Flight Service** (Java/Spring Boot) - 항공편 정보 및 가용성 관리
- **Ticket Service** (Kotlin/Spring Boot) - 티켓 예약 및 관리
- **Payment Service** (Kotlin/Spring Boot) - 결제 처리
- **Reservation Service** (Kotlin/Spring Boot) - 예약 워크플로우 관리
- **Jaeger Collector** - 추적 데이터 수집 및 저장
- **Jaeger Query** - 추적 데이터 조회를 위한 UI 및 API 제공

### 기술 스택
- **백엔드**: Java 17+, Kotlin, Spring Boot
- **분산 추적**: Jaeger (OpenTelemetry 호환)
- **컨테이너화**: Docker, Docker Compose
- **빌드 도구**: Gradle

### 시작하기

#### 사전 요구사항
- Docker 및 Docker Compose
- Java 17+
- Gradle

#### 애플리케이션 실행
1. 저장소 클론
```bash
git clone <repository-url>
cd jaeger-airline
```

2. 모든 서비스 시작
```bash
./build-and-run.sh
```

3. API 테스트 (완전한 예약 플로우)
```bash
./test-api.sh
```

4. 기존 Kafka 이벤트 방식 테스트
```bash
./request.sh
```

4. 서비스 접근:
    - Flight Service: http://localhost:8080
    - Ticket Service: http://localhost:8081
    - Payment Service: http://localhost:8082
    - Reservation Service: http://localhost:8083
    - Jaeger UI: http://localhost:16686

#### 서비스 포트
| 서비스 | 포트 |
|--------|------|
| Flight Service | 8080 |
| Ticket Service | 8081 |
| Payment Service | 8082 |
| Reservation Service | 8083 |
| Jaeger Collector | 4317 (gRPC), 4318 (HTTP) |
| Jaeger Query UI | 16686 |

### 🎯 새로운 기능: OpenFeign 기반 동기식 API 통신

#### REST API 엔드포인트

**Flight Service (항공편 조회)**
```http
GET  /v1/flights?from={출발지}&to={도착지}&date={날짜}
GET  /v1/flights/{flightId}
POST /v1/flights/{flightId}/availability
POST /v1/flights/{flightId}/reserve
POST /v1/flights/{flightId}/release
```

**Reservation Service (예약 오케스트레이션)**
```http
POST /v1/reservations                    # 완전한 예약 플로우
GET  /v1/reservations/{reservationId}    # 예약 조회
POST /v1/reservations/{reservationId}/cancel  # 예약 취소
```

**Payment Service (결제)**
```http
POST /v1/payments                        # 결제 요청
GET  /v1/payments/{paymentId}            # 결제 상태 조회
POST /v1/payments/{paymentId}/cancel     # 결제 취소
```

**Ticket Service (티켓 발급)**
```http
POST /v1/tickets                         # 티켓 발급
GET  /v1/tickets/{ticketId}              # 티켓 조회
POST /v1/tickets/{ticketId}/cancel       # 티켓 취소
```

#### 예약 요청 예시
```json
{
  "flightId": "KE123",
  "passengerInfo": {
    "name": "홍길동",
    "email": "hong@example.com",
    "phone": "010-1234-5678"
  },
  "seatPreference": "WINDOW",
  "paymentMethod": "CREDIT_CARD"
}
```

### 🔄 서비스 간 통신 플로우
1. **예약 요청**: Reservation Service가 모든 서비스를 오케스트레이션
2. **항공편 확인**: Flight Service에서 좌석 가용성 체크
3. **좌석 예약**: 임시 좌석 점유
4. **결제 처리**: Payment Service로 결제 진행
5. **티켓 발급**: 결제 성공 시 Ticket Service에서 티켓 생성
6. **예약 완료**: 모든 단계 성공 시 확정

### 🛡️ 장애 처리 및 복원력 패턴
- **Circuit Breaker**: 서비스 장애 시 빠른 실패 처리
- **보상 트랜잭션 (Saga)**: 부분 실패 시 롤백 처리
- **Retry**: 일시적 장애에 대한 재시도
- **Timeout**: 응답 지연 방지

### 분산 추적 테스트
1. **완전한 예약 플로우 테스트**
   ```bash
   ./test-api.sh
   ```
2. http://localhost:16686에서 Jaeger UI 열기
3. 추적 데이터를 검색하여 서비스 간 요청 플로우 시각화
4. 성능 병목지점 및 서비스 의존성 분석
5. Circuit Breaker 상태 확인: `http://localhost:8083/actuator/health`

### 프로젝트 구조
```
jaeger-airline/
├── flight/          # Flight 서비스 (Java)
├── ticket/          # Ticket 서비스 (Kotlin)
├── payment/         # Payment 서비스 (Kotlin)
├── reservation/     # Reservation 서비스 (Kotlin)
├── jaeger/          # Jaeger 설정
│   ├── collector/   # Jaeger collector 설정
│   └── query/       # Jaeger query 설정
└── docker-compose.yml
```

### 기여하기
이 프로젝트는 분산 추적 학습 및 검증을 위한 테스트베드입니다. 기여는 언제나 환영합니다!

### 라이선스
이 프로젝트는 오픈소스 프로젝트입니다.

---

## English

### Overview
This project is a distributed tracing testbed using **Jaeger**, designed to demonstrate and validate distributed tracing capabilities in a microservices architecture. The business domain is an airline reservation service with multiple interconnected services.

### Architecture
The testbed consists of the following microservices:
- **Flight Service** (Java/Spring Boot) - Manages flight information and availability
- **Ticket Service** (Kotlin/Spring Boot) - Handles ticket booking and management
- **Payment Service** (Kotlin/Spring Boot) - Processes payment transactions
- **Reservation Service** (Kotlin/Spring Boot) - Manages reservation workflow
- **Jaeger Collector** - Collects and stores tracing data
- **Jaeger Query** - Provides UI and API for querying traces

### Technology Stack
- **Backend**: Java 17+, Kotlin, Spring Boot
- **Tracing**: Jaeger (OpenTelemetry compatible)
- **Containerization**: Docker, Docker Compose
- **Build Tools**: Gradle

### Getting Started

#### Prerequisites
- Docker and Docker Compose
- Java 17+
- Gradle

#### Running the Application
1. Clone the repository
```bash
git clone <repository-url>
cd jaeger-airline
```

2. Start all services 
```bash
./build-and-run.sh
```

3. Sending an example request to verify distributed tracing
```bash
./request.sh
```

4. Access the services:
   - Flight Service: http://localhost:8080
   - Ticket Service: http://localhost:8081
   - Payment Service: http://localhost:8082
   - Reservation Service: http://localhost:8083
   - Jaeger UI: http://localhost:16686

#### Service Ports
| Service | Port |
|---------|------|
| Flight Service | 8080 |
| Ticket Service | 8081 |
| Payment Service | 8082 |
| Reservation Service | 8083 |
| Jaeger Collector | 4317 (gRPC), 4318 (HTTP) |
| Jaeger Query UI | 16686 |

### Testing Distributed Tracing
1. Make API calls to trigger service interactions
2. Open Jaeger UI at http://localhost:16686
3. Search for traces to visualize the request flow across services
4. Analyze performance bottlenecks and service dependencies

### Project Structure
```
jaeger-airline/
├── flight/          # Flight service (Java)
├── ticket/          # Ticket service (Kotlin)
├── payment/         # Payment service (Kotlin)
├── reservation/     # Reservation service (Kotlin)
├── jaeger/          # Jaeger configuration
│   ├── collector/   # Jaeger collector setup
│   └── query/       # Jaeger query setup
└── docker-compose.yml
```
