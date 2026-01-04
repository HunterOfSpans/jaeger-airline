# 보안 가이드

> **작성일**: 2026-01-04 | **버전**: Spring Boot 3.3.5, Jaeger 2.6.0 | **난이도**: 중급

---

## 목차

- [1. 개요](#1-개요)
- [2. Elasticsearch 보안](#2-elasticsearch-보안)
- [3. Kafka 보안](#3-kafka-보안)
- [4. Jaeger 보안](#4-jaeger-보안)
- [5. Spring Boot 서비스 보안](#5-spring-boot-서비스-보안)
- [6. Docker 컨테이너 보안](#6-docker-컨테이너-보안)
- [7. 네트워크 보안](#7-네트워크-보안)
- [8. 시크릿 관리](#8-시크릿-관리)
- [9. 체크리스트](#9-체크리스트)
- [10. 참고 자료](#10-참고-자료)

---

## 1. 개요

이 문서는 Jaeger 항공 예약 시스템의 보안 구성에 대한 가이드를 제공합니다. 개발 환경과 운영 환경에서의 보안 설정 차이점을 설명하고, 보안 모범 사례를 제시합니다.

### 1.1 보안 범위

| 구성 요소 | 개발 환경 | 운영 환경 |
|-----------|-----------|-----------|
| Elasticsearch | 인증 비활성화 | 인증 + TLS 필수 |
| Kafka | 인증 없음 | SASL + TLS 필수 |
| Jaeger | 공개 접근 | 인증 + TLS 권장 |
| 서비스 간 통신 | HTTP | HTTPS 권장 |

### 1.2 현재 구성의 한계

현재 프로젝트는 **개발/테스트 목적**으로 구성되어 있으며, 운영 환경에 배포하기 전에 반드시 보안 설정을 강화해야 합니다.

```yaml
# 현재 설정 (개발용) - 운영에서는 사용 금지!
elasticsearch:
  xpack.security.enabled: false  # ⚠️ 운영에서는 true 필수
```

---

## 2. Elasticsearch 보안

### 2.1 인증 활성화

```yaml
# docker-compose.yml (운영용)
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
  environment:
    - xpack.security.enabled=true
    - xpack.security.transport.ssl.enabled=true
    - xpack.security.http.ssl.enabled=true
    - ELASTIC_PASSWORD=${ELASTIC_PASSWORD}
```

### 2.2 인증서 생성

```bash
# Elasticsearch 인증서 생성
docker exec -it elasticsearch bin/elasticsearch-certutil ca
docker exec -it elasticsearch bin/elasticsearch-certutil cert --ca elastic-stack-ca.p12

# 인증서 복사
docker cp elasticsearch:/usr/share/elasticsearch/elastic-stack-ca.p12 ./certs/
```

### 2.3 Jaeger에서 인증된 Elasticsearch 연결

```yaml
# config-elasticsearch.yaml
extensions:
  jaeger_storage:
    backends:
      es:
        elasticsearch:
          server_urls: ["https://elasticsearch:9200"]
          tls:
            ca_file: /certs/ca.crt
          username: ${ES_USERNAME}
          password: ${ES_PASSWORD}
```

### 2.4 접근 권한 제어

```json
// Jaeger 전용 사용자 역할 생성
PUT /_security/role/jaeger_writer
{
  "cluster": ["monitor"],
  "indices": [
    {
      "names": ["jaeger-*"],
      "privileges": ["create_index", "write", "read", "delete"]
    }
  ]
}

// Jaeger 전용 사용자 생성
POST /_security/user/jaeger
{
  "password": "${JAEGER_ES_PASSWORD}",
  "roles": ["jaeger_writer"]
}
```

---

## 3. Kafka 보안

### 3.1 SASL 인증 구성

```yaml
# docker-compose-kafka.yml (운영용)
kafka1:
  environment:
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
    KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: PLAIN
    KAFKA_SASL_ENABLED_MECHANISMS: PLAIN
    KAFKA_OPTS: "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf"
```

### 3.2 JAAS 설정 파일

```
// kafka_server_jaas.conf
KafkaServer {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="admin"
  password="${KAFKA_ADMIN_PASSWORD}"
  user_admin="${KAFKA_ADMIN_PASSWORD}"
  user_jaeger="${KAFKA_JAEGER_PASSWORD}";
};
```

### 3.3 Spring Boot Kafka 클라이언트 설정

```kotlin
// KafkaProducerConfig.kt (운영용)
@Bean
fun producerFactory(): ProducerFactory<String, String> {
    val config = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        // 보안 설정
        "security.protocol" to "SASL_SSL",
        "sasl.mechanism" to "PLAIN",
        "sasl.jaas.config" to
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${kafkaUsername}\" password=\"${kafkaPassword}\";"
    )
    return DefaultKafkaProducerFactory(config)
}
```

### 3.4 TLS 암호화

```yaml
# application.yml (운영용)
spring:
  kafka:
    ssl:
      trust-store-location: classpath:truststore.jks
      trust-store-password: ${KAFKA_TRUSTSTORE_PASSWORD}
      key-store-location: classpath:keystore.jks
      key-store-password: ${KAFKA_KEYSTORE_PASSWORD}
```

---

## 4. Jaeger 보안

### 4.1 Jaeger UI 인증

Jaeger UI는 기본적으로 인증이 없습니다. 운영 환경에서는 리버스 프록시를 통한 인증이 권장됩니다.

```nginx
# nginx.conf
server {
    listen 443 ssl;
    server_name jaeger.example.com;

    ssl_certificate /etc/nginx/certs/jaeger.crt;
    ssl_certificate_key /etc/nginx/certs/jaeger.key;

    location / {
        auth_basic "Jaeger UI";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://jaeger:16686;
    }
}
```

### 4.2 Bearer Token 전파

```yaml
# config-elasticsearch.yaml
extensions:
  jaeger_query:
    bearer_token_propagation: true
```

### 4.3 OTLP 엔드포인트 보호

```yaml
# 운영 환경에서 OTLP 엔드포인트 접근 제한
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
        auth:
          authenticator: bearertokenauth
```

---

## 5. Spring Boot 서비스 보안

### 5.1 민감한 Actuator 엔드포인트 보호

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
        exclude: env,beans,configprops  # 민감한 정보 노출 금지
  endpoint:
    health:
      show-details: when_authorized

spring:
  security:
    user:
      name: ${ACTUATOR_USERNAME}
      password: ${ACTUATOR_PASSWORD}
```

### 5.2 환경 변수로 시크릿 관리

```kotlin
// 잘못된 예 - 하드코딩된 비밀번호
val password = "hardcoded-password"  // ❌ 절대 금지

// 올바른 예 - 환경 변수 사용
val password = System.getenv("DB_PASSWORD")  // ✅ 권장
```

### 5.3 민감한 데이터 로깅 방지

```kotlin
// LoggingAspect.kt
@Around("@annotation(Loggable)")
fun logMethodCall(pjp: ProceedingJoinPoint): Any? {
    val args = pjp.args.map { arg ->
        when (arg) {
            is PaymentRequest -> arg.copy(cardNumber = "****")  // 마스킹
            is String -> if (arg.contains("password")) "***" else arg
            else -> arg
        }
    }
    logger.info("Method: ${pjp.signature.name}, Args: $args")
    return pjp.proceed()
}
```

### 5.4 트레이싱에서 민감한 정보 제외

```kotlin
@KafkaOtelTrace(
    spanName = "process-payment",
    recordMessageContent = false,  // ⚠️ 결제 정보는 기록하지 않음
    attributes = ["service=payment"]  // 카드 번호 등 민감 정보 제외
)
fun processPayment(message: String, headers: MessageHeaders) {
    // 비즈니스 로직
}
```

---

## 6. Docker 컨테이너 보안

### 6.1 비루트 사용자 실행

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine

# 비루트 사용자 생성
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

USER appuser

COPY --chown=appuser:appgroup target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 6.2 읽기 전용 파일 시스템

```yaml
# docker-compose.yml
services:
  reservation:
    read_only: true
    tmpfs:
      - /tmp
    volumes:
      - ./logs:/app/logs:rw  # 로그만 쓰기 가능
```

### 6.3 리소스 제한

```yaml
# docker-compose.yml
services:
  reservation:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 6.4 이미지 취약점 스캔

```bash
# Trivy를 사용한 이미지 스캔
trivy image reservation-service:latest

# 고위험 취약점 발견 시 빌드 실패
trivy image --exit-code 1 --severity HIGH,CRITICAL reservation-service:latest
```

---

## 7. 네트워크 보안

### 7.1 Docker 네트워크 분리

```yaml
# docker-compose.yml
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true  # 외부 접근 차단

services:
  reservation:
    networks:
      - frontend
      - backend

  elasticsearch:
    networks:
      - backend  # 외부에서 직접 접근 불가
```

### 7.2 포트 노출 최소화

```yaml
# 개발 환경 - 모든 포트 노출 (디버깅용)
services:
  elasticsearch:
    ports:
      - "9200:9200"  # 개발에서만 허용

# 운영 환경 - 필요한 포트만 노출
services:
  elasticsearch:
    expose:
      - "9200"  # 내부 네트워크에서만 접근 가능
    # ports 없음 - 외부 노출 안 함
```

### 7.3 서비스 간 통신 암호화

```yaml
# application.yml (운영용)
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

---

## 8. 시크릿 관리

### 8.1 Docker Secrets (Swarm 모드)

```yaml
# docker-compose.yml
secrets:
  db_password:
    external: true
  kafka_password:
    external: true

services:
  reservation:
    secrets:
      - db_password
      - kafka_password
    environment:
      DB_PASSWORD_FILE: /run/secrets/db_password
```

### 8.2 환경 변수 파일

```bash
# .env (Git에 커밋 금지!)
ELASTIC_PASSWORD=secure-password-123
KAFKA_PASSWORD=another-secure-pwd
JAEGER_ES_PASSWORD=jaeger-password

# .gitignore에 추가
.env
*.env
```

### 8.3 Vault 통합 (권장)

```kotlin
// VaultConfig.kt
@Configuration
class VaultConfig {
    @Value("\${vault.uri}")
    lateinit var vaultUri: String

    @Bean
    fun vaultTemplate(): VaultTemplate {
        return VaultTemplate(
            VaultEndpoint.from(URI.create(vaultUri)),
            TokenAuthentication(System.getenv("VAULT_TOKEN"))
        )
    }
}

// 사용 예시
@Service
class SecretService(private val vaultTemplate: VaultTemplate) {
    fun getDbPassword(): String {
        val response = vaultTemplate.read("secret/data/database")
        return response?.data?.get("password") as String
    }
}
```

---

## 9. 체크리스트

### 9.1 배포 전 보안 체크리스트

#### 인증 및 인가
- [ ] Elasticsearch 인증 활성화
- [ ] Kafka SASL 인증 구성
- [ ] Jaeger UI 인증 설정 (리버스 프록시)
- [ ] Actuator 엔드포인트 보호
- [ ] 기본 비밀번호 변경

#### 암호화
- [ ] Elasticsearch TLS 활성화
- [ ] Kafka TLS 활성화
- [ ] 서비스 간 HTTPS 통신
- [ ] 데이터 저장 시 암호화

#### 시크릿 관리
- [ ] 하드코딩된 비밀번호 제거
- [ ] 환경 변수 또는 Vault 사용
- [ ] .env 파일 Git 제외

#### 컨테이너 보안
- [ ] 비루트 사용자 실행
- [ ] 이미지 취약점 스캔
- [ ] 리소스 제한 설정
- [ ] 읽기 전용 파일 시스템

#### 네트워크 보안
- [ ] 불필요한 포트 노출 제거
- [ ] 내부 네트워크 분리
- [ ] 방화벽 규칙 설정

#### 로깅 및 모니터링
- [ ] 민감한 데이터 로깅 방지
- [ ] 트레이싱에서 PII 제외
- [ ] 보안 이벤트 모니터링

### 9.2 정기 보안 점검

| 주기 | 점검 항목 |
|------|-----------|
| 일간 | 비정상 접근 로그 확인 |
| 주간 | 컨테이너 이미지 취약점 스캔 |
| 월간 | 인증서 만료 확인, 비밀번호 교체 |
| 분기 | 전체 보안 감사, 의존성 업데이트 |

---

## 10. 참고 자료

- [Elasticsearch Security](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-settings.html)
- [Kafka Security](https://kafka.apache.org/documentation/#security)
- [Jaeger Security](https://www.jaegertracing.io/docs/latest/security/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
