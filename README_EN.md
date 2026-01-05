# Jaeger Airline

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://openjdk.org/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Jaeger](https://img.shields.io/badge/Jaeger-2.14.1-blue.svg)](https://www.jaegertracing.io/)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-2.11.0-blueviolet.svg)](https://opentelemetry.io/)

**An airline reservation system testbed for learning and validating distributed tracing in MSA environments**

English | [한국어](README.md)

---

## About

Jaeger Airline is an educational project designed to practice and learn **Distributed Tracing** in a microservices architecture (MSA).

Four microservices simulate a real airline reservation system, allowing you to trace the entire request flow through **OpenTelemetry** and **Jaeger**. You can see how distributed tracing works in both synchronous communication (OpenFeign) and asynchronous messaging (Kafka).

### Key Features

- **Complete MSA Implementation**: 4 independent microservices (Flight, Payment, Ticket, Reservation)
- **Sync/Async Distributed Tracing**: Compare OpenFeign auto-instrumentation vs Kafka manual instrumentation
- **Resilience Patterns**: Circuit Breaker, Retry, Timeout (Resilience4j)
- **Compensation Transactions**: Saga pattern-based rollback handling
- **Shared Library**: Kafka tracing library shared via Gradle Composite Build
- **One-Click Deployment**: Entire infrastructure runs with Docker Compose

---

## Architecture

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

### Services

| Service | Port | Language | Role |
|---------|------|----------|------|
| **Flight Service** | 8080 | Java | Flight search, seat reservation/release |
| **Ticket Service** | 8081 | Kotlin | Ticket issuance/cancellation |
| **Payment Service** | 8082 | Kotlin | Payment processing/cancellation |
| **Reservation Service** | 8083 | Kotlin | Reservation orchestration, Saga pattern |

### Technology Stack

| Category | Technology | Version |
|----------|------------|---------|
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

## Quick Start

### Prerequisites

- Docker & Docker Compose v2+
- Java 21+
- Minimum 8GB RAM (Recommended: 16GB)

### 1. Clone the Repository

```bash
git clone https://github.com/HunterOfSpans/jaeger-airline.git
cd jaeger-airline
```

### 2. Build and Run All Services

```bash
# Build all services and start Docker containers
./script/build-and-run.sh
```

### 3. Verify Service Status

```bash
# Health check all services
for port in 8080 8081 8082 8083; do
  echo "localhost:$port - $(curl -s http://localhost:$port/actuator/health | jq -r '.status')"
done
```

### 4. Test Distributed Tracing

```bash
# Test OpenFeign synchronous call tracing
./script/test-feign-tracing.sh

# Test Kafka asynchronous messaging tracing
./script/test-kafka-tracing.sh
```

### 5. View Traces in Jaeger UI

1. Open http://localhost:16686
2. Select Service: `reservation-service`
3. Click **Find Traces**
4. Select a trace to view the complete call flow

---

## API Endpoints

### Reservation Service (8083) - Orchestration

```http
POST /v1/reservations                    # Create reservation
GET  /v1/reservations/{id}               # Get reservation
POST /v1/reservations/{id}/cancel        # Cancel reservation
```

### Distributed Tracing Test Endpoints

```http
# OpenFeign Synchronous Tracing
POST /v1/tracing/feign/simple-flow       # Simple call chain
POST /v1/tracing/feign/complex-flow      # Complex reservation process
POST /v1/tracing/feign/parallel-calls    # Parallel calls

# Kafka Asynchronous Tracing
POST /v1/tracing/kafka/simple-events     # Simple event chain
POST /v1/tracing/kafka/complex-events    # Complex event flow
POST /v1/tracing/kafka/failure-compensation  # Failure/compensation transaction
```

### Flight Service (8080)

```http
GET  /v1/flights                         # Search flights
GET  /v1/flights/{flightId}              # Get flight details
POST /v1/flights/{flightId}/availability # Check seat availability
POST /v1/flights/{flightId}/reserve      # Reserve seat
POST /v1/flights/{flightId}/release      # Release seat
```

### Payment Service (8082)

```http
POST /v1/payments                        # Process payment
GET  /v1/payments/{paymentId}            # Get payment
POST /v1/payments/{paymentId}/cancel     # Cancel payment
```

### Ticket Service (8081)

```http
POST /v1/tickets                         # Issue ticket
GET  /v1/tickets/{ticketId}              # Get ticket
POST /v1/tickets/{ticketId}/cancel       # Cancel ticket
```

---

## Distributed Tracing

This project implements two approaches to distributed tracing:

### 1. OpenFeign Auto-Instrumentation

Trace context is **automatically** propagated through OpenFeign + Micrometer Tracing.

```kotlin
@FeignClient(name = "flight-service")
interface FlightClient {
    @PostMapping("/v1/flights/{flightId}/reserve")
    fun reserveSeat(@PathVariable flightId: String): SeatResponse
}
// HTTP headers (traceparent, tracestate) are automatically propagated
```

### 2. Kafka Manual Instrumentation

Kafka messaging requires **manual instrumentation** with `@KafkaOtelTrace` annotation and AOP.

```kotlin
@KafkaOtelTrace(spanName = "process-payment-event")
@KafkaListener(topics = ["reservation.requested"])
fun handleReservation(message: String, @Headers headers: MessageHeaders) {
    // AOP extracts trace context from Kafka headers
    // New span is linked to the existing trace
}
```

### Kafka Event Chain

```
reservation.requested → seat.reserved → payment.approved → ticket.issued → reservation.completed
```

For more details, see the [Distributed Tracing Overview](docs/guides/distributed-tracing-overview.md) documentation.

---

## Project Structure

```
jaeger-airline/
├── common/
│   └── kafka-tracing/          # Shared Kafka tracing library
│       ├── annotation/         # @KafkaOtelTrace
│       ├── aspect/             # KafkaTracingAspect (AOP)
│       └── config/             # Spring Boot auto-configuration
├── flight/                     # Flight Service (Java)
├── payment/                    # Payment Service (Kotlin)
├── ticket/                     # Ticket Service (Kotlin)
├── reservation/                # Reservation Service (Kotlin)
├── jaeger/
│   ├── collector/              # Jaeger Collector configuration
│   └── query/                  # Jaeger Query configuration
├── docs/                       # Technical documentation
│   ├── getting-started/        # Getting started guides
│   ├── architecture/           # Architecture design
│   ├── guides/                 # Practical guides
│   ├── reference/              # Technical reference
│   └── troubleshooting/        # Troubleshooting
├── script/                     # Build/test scripts
├── docker-compose.yml          # Jaeger + Elasticsearch
└── docker-compose-kafka.yml    # Kafka cluster
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [Apply to Your Project](docs/getting-started/apply-to-your-project.md) | **Step-by-step setup guide** (copy & paste) |
| [Concepts Guide](docs/getting-started/jaeger-otel.md) | Spring Boot + OTel + Jaeger theory |
| [Distributed Tracing Overview](docs/guides/distributed-tracing-overview.md) | How tracing works end-to-end |
| [OpenFeign Tracing Guide](docs/guides/OpenFeign-Distributed-Tracing-Guide.md) | Synchronous communication tracing |
| [Kafka Tracing Guide](docs/guides/Kafka-Distributed-Tracing-Complete-Guide.md) | Asynchronous messaging tracing |
| [Library Sharing Guide](docs/architecture/library-sharing-guide.md) | Gradle Composite Build |
| [ES Mapping Conflict Resolution](docs/troubleshooting/elasticsearch-mapping-conflict.md) | Troubleshooting |

See [docs/README.md](docs/README.md) for the complete documentation index.

---

## Monitoring UI

| Service | URL | Purpose |
|---------|-----|---------|
| **Jaeger UI** | http://localhost:16686 | Distributed tracing visualization |
| **Kafka UI** | http://localhost:8085 | Message queue monitoring |
| **Kibana** | http://localhost:5601 | Log analysis |

---

## Learning Objectives

Through this project, you can learn:

1. **Distributed Tracing Systems** - OpenTelemetry SDK + Jaeger backend setup
2. **Auto vs Manual Instrumentation** - Compare OpenFeign auto-tracing with Kafka manual tracing
3. **MSA Communication Patterns** - Synchronous (REST) / Asynchronous (messaging) patterns
4. **Resilience Patterns** - Circuit Breaker, Retry, Timeout patterns
5. **Compensation Transactions** - Maintaining distributed transaction consistency with Saga pattern
6. **Observability** - Debugging and performance analysis of distributed systems

---

## Scripts

```bash
./script/build-and-run.sh        # Build and run
./script/rebuild-and-restart.sh  # Including Docker image rebuild
./script/request.sh              # Basic reservation flow test
./script/test-api.sh             # Full API test
./script/test-feign-tracing.sh   # OpenFeign tracing test
./script/test-kafka-tracing.sh   # Kafka tracing test
```

---

## Contributing

Issues and Pull Requests are welcome!

1. Fork this repository
2. Create a new branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Create a Pull Request

### Development Environment Setup

```bash
# Run infrastructure only (for local development)
docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d elasticsearch kafka1 kafka2 kafka3 jaeger-collector jaeger-query

# Run a specific service locally
cd reservation && ./gradlew bootRun
```

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Acknowledgements

- [Jaeger](https://www.jaegertracing.io/) - Distributed tracing backend
- [OpenTelemetry](https://opentelemetry.io/) - Observability framework
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Resilience4j](https://resilience4j.readme.io/) - Resilience library
