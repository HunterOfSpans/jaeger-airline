# Elasticsearch 인덱스 매핑 충돌 문제 해결

> **작성일**: 2026-01-04 | **버전**: Jaeger v2, Elasticsearch 8.x | **난이도**: 중급

---

## 목차

- [1. 문제 현상](#1-문제-현상)
- [2. 원인 분석](#2-원인-분석)
- [3. 해결 방법](#3-해결-방법)
- [4. 예방 방법](#4-예방-방법)
- [5. 검증 방법](#5-검증-방법)
- [6. 관련 로그 위치](#6-관련-로그-위치)
- [7. 참고 자료](#7-참고-자료)

---

## 1. 문제 현상

### 1.1 에러 메시지

```
Elasticsearch part of bulk request failed
{
  "_index": "jaeger-prod-jaeger-span-write",
  "status": 400,
  "error": {
    "type": "illegal_argument_exception",
    "reason": "mapper [process.tags.value] cannot be changed from type [text] to [long]"
  }
}
```

### 1.2 증상

- Jaeger Collector 로그에서 위 에러 반복 발생
- Jaeger UI에서 서비스/트레이스 조회 불가 (`/api/services` 빈 배열 반환)
- Span은 Collector로 정상 전송됨 (`Wrote span to ES index` 로그 확인)
- Elasticsearch에 데이터 저장 실패

---

## 2. 원인 분석

### 2.1 Elasticsearch 매핑의 특성

Elasticsearch는 **dynamic mapping**을 사용하여 처음 들어오는 데이터 타입으로 필드 타입을 자동 결정한다.

```
첫 번째 데이터: { "process.tags.value": "hello" }  → text 타입으로 매핑
두 번째 데이터: { "process.tags.value": 12345 }    → long 타입 시도 → 충돌!
```

### 2.2 Jaeger Span 데이터의 특성

Jaeger의 `process.tags`는 다양한 타입의 값을 가질 수 있다:

| 태그 | 값 예시 | 추론 타입 |
|------|---------|----------|
| `service.name` | `"reservation"` | text |
| `telemetry.sdk.version` | `"1.36.0"` | text |
| `process.pid` | `12345` | long |
| `thread.id` | `7` | long |

### 2.3 충돌 발생 시나리오

```
시간순서:

1. reservation-service 시작
   → process.tags.value = "reservation" (text)
   → ES 매핑: text 타입으로 고정

2. flight-service 시작
   → process.tags.value = 12345 (PID)
   → ES 매핑 충돌! (text → long 변경 불가)

3. 모든 후속 span 저장 실패
```

### 2.4 왜 이 문제가 발생했나?

1. **Jaeger 인덱스 템플릿 부재**: Jaeger가 ES에 적절한 인덱스 템플릿을 생성하지 못함
2. **Dynamic Mapping 의존**: 첫 데이터가 매핑을 결정하는 구조
3. **이전 데이터 잔존**: Docker 볼륨에 이전 세션의 매핑이 남아있음

---

## 3. 해결 방법

### 3.1 방법 1: Docker 볼륨 완전 초기화 (권장)

가장 확실한 방법. 모든 데이터와 매핑을 초기화한다.

```bash
# 모든 컨테이너 중지 및 볼륨 삭제
docker compose -f docker-compose-kafka.yml -f docker-compose.yml down -v

# 다시 시작
docker compose -f docker-compose-kafka.yml -f docker-compose.yml up -d
```

**장점**: 깔끔한 초기화
**단점**: 모든 기존 트레이스 데이터 삭제

### 3.2 방법 2: Jaeger 인덱스만 삭제

기존 Kafka 데이터는 유지하고 Jaeger 인덱스만 삭제.

```bash
# Jaeger 인덱스 확인
curl -s "http://localhost:9200/_cat/indices?v" | grep jaeger

# 인덱스 삭제 (하나씩)
curl -X DELETE "http://localhost:9200/jaeger-prod-jaeger-span-write"
curl -X DELETE "http://localhost:9200/jaeger-prod-jaeger-service-write"

# Collector 재시작으로 새 인덱스 생성
docker restart collector query
```

**주의**: 인덱스 템플릿이 없으면 같은 문제 재발 가능

### 3.3 방법 3: 인덱스 템플릿 수동 생성 (영구 해결)

Jaeger 공식 인덱스 템플릿을 수동으로 생성하여 영구적으로 해결.

```bash
# Jaeger span 인덱스 템플릿 생성
curl -X PUT "http://localhost:9200/_index_template/jaeger-span" \
  -H "Content-Type: application/json" \
  -d '{
    "index_patterns": ["jaeger-*-jaeger-span-*"],
    "template": {
      "mappings": {
        "dynamic_templates": [
          {
            "span_tags_map": {
              "mapping": {
                "type": "keyword",
                "ignore_above": 256
              },
              "path_match": "tag.*"
            }
          },
          {
            "process_tags_map": {
              "mapping": {
                "type": "keyword",
                "ignore_above": 256
              },
              "path_match": "process.tags.*"
            }
          }
        ]
      }
    }
  }'
```

이 템플릿은 `process.tags.*` 필드를 항상 `keyword` 타입으로 매핑하여 타입 충돌을 방지한다.

---

## 4. 예방 방법

### 4.1 ILM (Index Lifecycle Management) 설정

`docker-compose.yml`에 ILM 설정 컨테이너가 있는지 확인:

```yaml
ilm-setup:
  image: curlimages/curl:latest
  depends_on:
    elasticsearch:
      condition: service_healthy
  command: >
    sh -c "curl -X PUT 'http://elasticsearch:9200/_ilm/policy/jaeger-ilm-policy' ..."
```

### 4.2 정기적인 인덱스 관리

```bash
# 오래된 인덱스 삭제 (30일 이상)
curl -X DELETE "http://localhost:9200/jaeger-*-$(date -d '-30 days' +%Y-%m-%d)"
```

### 4.3 Jaeger 버전 업그레이드 시

Jaeger 버전 업그레이드 시 인덱스 스키마가 변경될 수 있으므로:

1. 기존 인덱스 백업 또는 삭제
2. 새 버전으로 업그레이드
3. 인덱스 템플릿 확인

---

## 5. 검증 방법

### 5.1 서비스 등록 확인

```bash
curl -s "http://localhost:16686/api/services" | jq '.data'
# 예상 결과: ["flight", "payment", "reservation", "ticket"]
```

### 5.2 트레이스 연결 확인

```bash
# Kafka 이벤트 발생
curl -X POST http://localhost:8083/v1/tracing/kafka/simple-events

# 8초 후 트레이스 확인
sleep 8
curl -s "http://localhost:16686/api/traces?service=reservation&limit=1" | \
  jq '.data[0] | {traceID, spanCount: (.spans | length), services: [.processes[].serviceName] | unique}'
```

**정상 결과:**
```json
{
  "traceID": "ca9c8500f0bacb62e56b01127985a6ae",
  "spanCount": 13,
  "services": ["flight", "payment", "reservation", "ticket"]
}
```

4개 서비스가 하나의 traceID로 연결되어 있으면 정상.

---

## 6. 관련 로그 위치

| 컴포넌트 | 로그 명령어 |
|---------|------------|
| Collector | `docker logs collector` |
| Query | `docker logs query` |
| Elasticsearch | `docker logs elasticsearch` |
| 서비스 | `docker logs reservation` |

### 6.1 Collector 정상 로그 예시

```
Wrote span to ES index  {"index": "jaeger-prod-jaeger-span-write"}
```

### 6.2 Collector 에러 로그 예시

```
Elasticsearch part of bulk request failed  {"error": {"reason": "mapper [process.tags.value] cannot be changed..."}}
```

---

## 7. 참고 자료

- [Elasticsearch Dynamic Mapping](https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-mapping.html)
- [Jaeger Elasticsearch Configuration](https://www.jaegertracing.io/docs/latest/deployment/#elasticsearch)
- [Jaeger v2 Migration Guide](https://www.jaegertracing.io/docs/latest/migration/)
