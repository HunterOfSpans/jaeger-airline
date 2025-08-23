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

3. 분산 추적 확인을 위한 예시 요청 전송
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

### 분산 추적 테스트
1. API 호출을 통해 서비스 간 상호작용 유발
2. http://localhost:16686에서 Jaeger UI 열기
3. 추적 데이터를 검색하여 서비스 간 요청 플로우 시각화
4. 성능 병목지점 및 서비스 의존성 분석

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
