# Jaeger Airline - Claude Code 프로젝트 정보

## 프로젝트 개요
- **이름**: Jaeger 항공 예약 시스템 (MSA 분산 추적 테스트베드)
- **언어**: Java 21 (Flight Service), Kotlin (Payment, Ticket, Reservation Services)  
- **프레임워크**: Spring Boot 3.3.5, Spring Cloud 2023.0.3
- **빌드 도구**: Gradle
- **아키텍처**: 마이크로서비스 아키텍처 (MSA)

## 주요 기술 스택
- **분산 추적**: Jaeger + OpenTelemetry
- **서비스 통신**: OpenFeign (동기), Apache Kafka (비동기)
- **장애 복원력**: Resilience4j (Circuit Breaker, Retry, Timeout)
- **컨테이너화**: Docker & Docker Compose
- **모니터링**: Elasticsearch, Kibana

## 서비스 구성
1. **Flight Service** (8080) - Java/Spring Boot - 항공편 정보 및 좌석 관리
2. **Payment Service** (8082) - Kotlin/Spring Boot - 결제 처리 및 검증  
3. **Ticket Service** (8081) - Kotlin/Spring Boot - 항공권 발급 및 관리
4. **Reservation Service** (8083) - Kotlin/Spring Boot - 예약 오케스트레이션 및 Saga 패턴

## 모니터링 도구
- **Jaeger UI**: http://localhost:16686 - 분산 추적 확인
- **Kafka UI**: http://localhost:8085 - 메시징 상태 확인
- **Kibana**: http://localhost:5601 - 로그 분석

## 프로젝트 빌드 및 실행
### 빠른 시작
```bash
./script/build-and-run.sh  # 모든 서비스 빌드 및 실행
```

### 개별 서비스 빌드
```bash
cd flight && ./gradlew build -x test
cd payment && ./gradlew build -x test  
cd ticket && ./gradlew build -x test
cd reservation && ./gradlew build -x test
```

### Docker Compose 실행
```bash
docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d
```

## 테스트 명령어
### API 테스트
```bash
./script/request.sh             # 전체 예약 플로우 테스트
./script/test-api.sh            # API 엔드포인트 테스트
```

### 헬스 체크
```bash
curl http://localhost:8080/actuator/health  # Flight Service
curl http://localhost:8081/actuator/health  # Ticket Service  
curl http://localhost:8082/actuator/health  # Payment Service
curl http://localhost:8083/actuator/health  # Reservation Service
```

### Circuit Breaker 상태 확인
```bash
curl http://localhost:8083/actuator/circuitbreakers
```

## 주요 API 엔드포인트

### Flight Service (8080)
- `GET /v1/flights?from={출발지}&to={도착지}&date={날짜}` - 항공편 검색
- `POST /v1/flights/{flightId}/availability` - 좌석 가용성 확인
- `POST /v1/flights/{flightId}/reserve` - 좌석 예약

### Reservation Service (8083) - 메인 오케스트레이션
- `POST /v1/reservations` - 완전한 예약 플로우 실행
- `GET /v1/reservations/{reservationId}` - 예약 상태 조회

### Payment Service (8082)
- `POST /v1/payments` - 결제 처리
- `POST /v1/payments/{paymentId}/cancel` - 결제 취소

### Ticket Service (8081)
- `POST /v1/tickets` - 항공권 발급
- `POST /v1/tickets/{ticketId}/cancel` - 항공권 취소

## 주요 패턴 구현
- **Circuit Breaker**: Resilience4j로 장애 복원력 구현
- **Saga Pattern**: 분산 트랜잭션 보상 로직 구현
- **OpenFeign**: 서비스 간 동기 통신
- **Kafka Messaging**: 비동기 이벤트 처리

## 분산 추적 확인 방법
1. Jaeger UI (http://localhost:16686) 접속
2. Service: `reservation-service` 선택
3. Operation: `POST /v1/reservations` 선택
4. Find Traces 클릭하여 전체 요청 플로우 확인

## 프로젝트 구조
```
jaeger-airline/
├── common/
│   └── kafka-tracing/         # 공통 Kafka 트레이싱 라이브러리
│       ├── annotation/        # @KafkaOtelTrace 어노테이션
│       ├── aspect/            # KafkaTracingAspect (AOP)
│       └── config/            # 자동 설정
├── flight/                    # Flight Service (Java)
├── payment/                   # Payment Service (Kotlin)
├── ticket/                    # Ticket Service (Kotlin)
├── reservation/               # Reservation Service (Kotlin)
├── docs/
│   ├── getting-started/       # 시작하기 가이드
│   ├── architecture/          # 아키텍처 설계 문서
│   ├── guides/                # 실습 가이드 (보안, 트레이싱)
│   ├── reference/             # 기술 참조 문서
│   └── troubleshooting/       # 문제 해결 가이드
├── script/                    # 빌드/테스트 스크립트
├── docker-compose-kafka.yml   # Kafka 클러스터 설정
├── docker-compose.yml         # Jaeger 및 전체 인프라
└── README.md                  # 상세 문서
```

## 공통 라이브러리 (Gradle Composite Build)
각 서비스는 `common/kafka-tracing` 모듈을 의존성으로 사용:
```kotlin
// settings.gradle.kts
includeBuild("../common/kafka-tracing")

// build.gradle.kts
implementation("com.airline:kafka-tracing")
```
자세한 내용: `docs/architecture/library-sharing-guide.md`

## 분산 추적 테스트 명령어

### OpenFeign 동기 추적 테스트
```bash
./script/test-feign-tracing.sh   # OpenFeign 기반 동기 호출 체인 테스트
```
**엔드포인트:**
- `POST /v1/tracing/feign/simple-flow` - 간단한 동기 호출 체인
- `POST /v1/tracing/feign/complex-flow` - 복잡한 예약 프로세스 (Circuit Breaker 포함)
- `POST /v1/tracing/feign/circuit-breaker-test` - Circuit Breaker 동작 테스트
- `POST /v1/tracing/feign/parallel-calls` - 병렬 호출 테스트

### Kafka 비동기 추적 테스트
```bash
./script/test-kafka-tracing.sh   # Kafka 기반 이벤트 체인 테스트
```
**엔드포인트:**
- `POST /v1/tracing/kafka/simple-events` - 간단한 이벤트 체인
- `POST /v1/tracing/kafka/complex-events` - 복잡한 이벤트 플로우
- `POST /v1/tracing/kafka/failure-compensation` - 실패/보상 트랜잭션
- `POST /v1/tracing/kafka/multi-topic-events` - 다중 토픽 이벤트
- `GET /v1/tracing/kafka/event-status/{eventId}` - 이벤트 처리 상태 조회

### 기본 명령어
- `./gradlew build` - 프로젝트 빌드
- `docker compose up -d` - 서비스 시작
- `curl [API_URL]` - API 테스트
- `docker logs [container_name]` - 로그 확인

## 중요 설정 파일
- **Circuit Breaker 설정**: `reservation/src/main/resources/application-circuit.yml`
- **OpenTelemetry 설정**: 각 서비스의 `OpenTelemetryConfig.java/kt`
- **Kafka 설정**: 각 서비스의 `KafkaProducerConfig.java/kt`

## 개발 가이드라인

### 코드 스타일
- **Java**: Google Java Style Guide 준수
- **Kotlin**: 공식 Kotlin Coding Conventions 준수
- **빌드**: `./gradlew build` 성공 후 코드 변경 완료
- **테스트**: `./gradlew test` 실행하여 회귀 테스트 확인

### 분산 추적 구현 원칙
- **Span 생성**: 각 서비스 경계마다 새로운 span 생성
- **컨텍스트 전파**: HTTP 헤더와 Kafka 헤더를 통한 trace context 전파
- **에러 처리**: 예외 발생 시 span에 에러 정보 기록
- **메타데이터**: 비즈니스 로직 관련 태그 추가 (flightId, reservationId 등)

### 서비스 간 통신 패턴
- **동기 통신**: OpenFeign + Circuit Breaker
- **비동기 통신**: Kafka + 멱등성 보장
- **장애 대응**: Saga 패턴으로 보상 트랜잭션 구현

### 로컬 개발 환경 설정
```bash
# 인프라 서비스만 시작 (개발 시)
docker compose -f docker-compose-kafka.yml up -d

# 특정 서비스만 재빌드
cd [service-name] && ./gradlew bootRun

# 전체 시스템 재시작
./script/rebuild-and-restart.sh
```