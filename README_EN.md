# Jaeger Airline Reservation System | MSA Distributed Tracing Testbed

This project is a distributed tracing verification system using **Jaeger**, designed to demonstrate and validate distributed tracing capabilities in a complete microservices architecture (MSA).

## 🏗️ Architecture Overview

### Microservices Components
- **Flight Service** (Java/Spring Boot) - Flight information and seat management
- **Payment Service** (Kotlin/Spring Boot) - Payment processing and validation  
- **Ticket Service** (Kotlin/Spring Boot) - Airline ticket issuance and management
- **Reservation Service** (Kotlin/Spring Boot) - Reservation orchestration and Saga pattern implementation

### Core Technology Stack
- **Backend**: Java 17+, Kotlin, Spring Boot 3.3.5, Spring Cloud 2023.0.3
- **Service Communication**: OpenFeign (synchronous), Kafka (asynchronous)
- **Resilience Patterns**: Resilience4j (Circuit Breaker, Retry, Timeout)
- **Distributed Tracing**: Jaeger + OpenTelemetry
- **Messaging**: Apache Kafka Cluster (3-node)
- **Containerization**: Docker & Docker Compose
- **Build**: Gradle

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Gradle 7+

### 1. Environment Setup
```bash
git clone https://github.com/HunterOfSpans/jaeger-airline.git
cd jaeger-airline
```

### 2. Start Kafka Cluster
```bash
docker-compose -f docker-compose-kafka.yml up -d
```

### 3. Build and Run Services
```bash
# Run each service in separate terminals
cd flight && ./gradlew bootRun --args='--spring.profiles.active=local'
cd payment && ./gradlew bootRun --args='--spring.profiles.active=local'  
cd ticket && ./gradlew bootRun --args='--spring.profiles.active=local'
cd reservation && ./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. Service Access Points
| Service | Port | Health Check |
|---------|------|-------------|
| Flight Service | 8080 | http://localhost:8080/actuator/health |
| Ticket Service | 8081 | http://localhost:8081/actuator/health |
| Payment Service | 8082 | http://localhost:8082/actuator/health |
| Reservation Service | 8083 | http://localhost:8083/actuator/health |
| Jaeger UI | 16686 | http://localhost:16686 |
| Kafka UI | 8085 | http://localhost:8085 |

## 🔄 MSA Pattern Implementations

### 1. OpenFeign-based Inter-Service Communication
```kotlin
@FeignClient(name = "payment-service", url = "\${services.payment.url}")
interface PaymentClient {
    @PostMapping("/v1/payments")
    fun processPayment(@RequestBody request: PaymentRequest): PaymentResponse
}
```

### 2. Circuit Breaker Pattern (Resilience4j)
```kotlin
@CircuitBreaker(name = "reservation", fallbackMethod = "createReservationFallback")
fun createReservation(request: ReservationRequest): ReservationResponse {
    // Service orchestration logic
}
```

### 3. Saga Pattern (Compensation Transactions)
```kotlin
private fun executeCompensation(reservationId: String, flightId: String) {
    logger.info("Executing compensation for reservation: {}", reservationId)
    // 1. Cancel ticket
    // 2. Cancel payment
    // 3. Release seats
}
```

## 📋 REST API Endpoints

### Flight Service
```http
GET  /v1/flights?from={departure}&to={arrival}&date={date}  # Search flights
GET  /v1/flights/{flightId}                                 # Get flight details
POST /v1/flights/{flightId}/availability                    # Check seat availability
POST /v1/flights/{flightId}/reserve                        # Reserve seats
POST /v1/flights/{flightId}/release                        # Release seats
```

### Reservation Service (Orchestration)
```http
POST /v1/reservations                         # Execute complete reservation flow
GET  /v1/reservations/{reservationId}         # Get reservation status
POST /v1/reservations/{reservationId}/cancel  # Cancel reservation
```

### Payment Service
```http
POST /v1/payments                        # Process payment
GET  /v1/payments/{paymentId}            # Get payment details
POST /v1/payments/{paymentId}/cancel     # Cancel payment
```

### Ticket Service
```http
POST /v1/tickets                         # Issue airline ticket
GET  /v1/tickets/{ticketId}              # Get ticket details
POST /v1/tickets/{ticketId}/cancel       # Cancel ticket
```

## 🎯 Complete Reservation Flow Testing

### 1. Success Scenario
```bash
curl -X POST http://localhost:8083/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "flightId": "OZ456",
    "passengerInfo": {
      "name": "John Passenger",
      "email": "passenger@example.com",
      "phone": "010-1234-5678"
    },
    "seatPreference": "window",
    "paymentMethod": "CARD"
  }'
```

**Expected Response:**
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

## 🔍 Jaeger Distributed Tracing Verification

### 1. View Tracing Data
1. **Access Jaeger UI**: http://localhost:16686
2. **Select Service**: `reservation-service`
3. **Select Operation**: `POST /v1/reservations`
4. **Click Find Traces**

### 2. Available Tracing Information
- **Complete Request Flow**: Reservation → Flight → Payment → Ticket
- **Individual Service Response Times**: Performance analysis per service
- **Error Tracking**: Failure points and stack traces
- **Service Dependency Map**: Visual representation of service call relationships

### 3. Performance Metrics Analysis
- **Total Duration**: Complete reservation processing time
- **Service Time**: Processing time per service
- **Network Latency**: Inter-service network delays
- **Database Operations**: Data processing time

## 🛡️ Resilience Pattern Verification

### 1. Circuit Breaker Testing
```bash
# Stop Payment service and attempt reservation
# Circuit Breaker automatically activates for fast failure handling
curl -s http://localhost:8083/actuator/circuitbreakers | jq .
```

### 2. Compensation Transaction Testing
```bash
# Verify automatic rollback on payment failure scenarios
# 1. Release seats
# 2. Cancel issued tickets
# 3. Maintain data consistency
```

### 3. Kafka Messaging Verification
```bash
# Verify event publishing per service
# - reservation.created (reservation completed)
# - payment.approved (payment approved)
# - ticket.issued (ticket issued)
```

## 📊 Monitoring and Management

### Circuit Breaker Status Monitoring
```bash
curl http://localhost:8083/actuator/circuitbreakers
```

### Service Health Checks
```bash
curl http://localhost:8083/actuator/health
```

### Kafka Consumer Status Verification
- Kafka UI: http://localhost:8085
- Topics: `reservation.created`, `payment.approved`, `ticket.issued`

## 🧪 Test Scenarios

### 1. Normal Flow Testing
- ✅ Flight search → Seat reservation → Payment → Ticket issuance

### 2. Failure Scenario Testing
- ❌ Payment service down → Circuit Breaker activation
- ❌ Payment failure → Compensation transaction execution
- ❌ Network delay → Timeout handling

### 3. Concurrency Testing
- 🔄 Multiple concurrent reservation requests
- 🔄 Seat competition scenario handling

## 📁 Project Structure

```
jaeger-airline/
├── flight/                     # Flight Service (Java)
│   ├── src/main/java/
│   │   ├── controller/         # REST API controllers
│   │   ├── service/           # Business logic
│   │   └── dto/               # Data transfer objects
│   └── src/main/resources/
│       ├── application.yml    # Base configuration
│       └── application-local.yml # Local environment settings
├── payment/                    # Payment Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── controller/        # REST API controllers
│   │   ├── service/          # Payment business logic
│   │   ├── dto/              # Payment DTOs
│   │   └── config/           # Kafka Producer configuration
│   └── src/main/resources/
├── ticket/                     # Ticket Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── controller/       # Ticket API
│   │   ├── service/         # Ticket issuance logic
│   │   └── dto/             # Ticket DTOs
│   └── src/main/resources/
├── reservation/               # Reservation Service (Kotlin)
│   ├── src/main/kotlin/
│   │   ├── api/             # Reservation API
│   │   ├── service/         # Orchestration logic
│   │   ├── client/          # OpenFeign clients
│   │   ├── dto/             # Reservation DTOs
│   │   └── config/          # Circuit Breaker configuration
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── application-circuit.yml # Circuit Breaker settings
├── docker-compose-kafka.yml   # Kafka cluster configuration
├── docker-compose.yml         # Jaeger and infrastructure
└── README.md
```

## 🎓 Learning Objectives

Through this project, you can learn:

1. **Distributed Tracing Systems** - Jaeger + OpenTelemetry
2. **Microservice Communication** - OpenFeign, Kafka
3. **Resilience Patterns** - Circuit Breaker, Saga Pattern
4. **Service Orchestration** - Complex business flow management
5. **Monitoring and Observability** - Distributed system debugging

## 🤝 Contributing

This project is an educational project for learning MSA and distributed tracing.

### Improvement Ideas
- [ ] Distributed Caching (Redis)
- [ ] API Gateway (Spring Cloud Gateway)
- [ ] Service Mesh (Istio)
- [ ] Event Sourcing Pattern
- [ ] CQRS Pattern

## 📄 License

This project is distributed under the MIT License.

---

📚 **For more details, see the [Korean Documentation](README.md).**