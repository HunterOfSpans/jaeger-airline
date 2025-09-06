# Jaeger 버전별 ILM(Index Lifecycle Management) 호환성 분석

> 분산 추적을 위한 Jaeger와 Elasticsearch ILM(Index Lifecycle Management) 정책 적용 시 발생하는 버전별 호환성 문제와 해결 방안에 대한 상세 분석

## 🎯 목차

1. [문제 상황 개요](#문제-상황-개요)
2. [ILM이란 무엇인가?](#ilm이란-무엇인가)
3. [Jaeger 배포 방식 이해하기](#jaeger-배포-방식-이해하기)
4. [Jaeger 버전별 테스트 결과](#jaeger-버전별-테스트-결과)
5. [문제 원인 심층 분석](#문제-원인-심층-분석)
6. [실제 검증 결과](#실제-검증-결과)
7. [ILM 지원 상태 확인 방법](#ilm-지원-상태-확인-방법)
8. [해결 방안 및 권장사항](#해결-방안-및-권장사항)
9. [참고 자료](#참고-자료)

---

## 문제 상황 개요

Jaeger를 Elasticsearch와 함께 사용할 때 ILM(Index Lifecycle Management)을 통해 인덱스 생명주기를 자동으로 관리하려는 시도에서 버전별로 다른 동작을 보이는 문제를 발견했습니다.

### 🚨 핵심 오류 메시지

```bash
invalid configuration: extensions::jaeger_storage::backends::es::elasticsearch: 
when UseILM is set true, CreateIndexTemplates must be set to false and index templates 
must be created by init process of es-rollover app
```

이 오류는 **Jaeger 2.7.0 이상**에서 ILM 설정 시 발생하며, OpenTelemetry Collector 기반 배포에서 해결할 수 없는 구조적 제약사항입니다.

---

## ILM이란 무엇인가?

### 📝 ILM(Index Lifecycle Management) 개념

ILM은 Elasticsearch의 기능으로, 인덱스의 생명주기를 자동으로 관리합니다:

- **Hot Phase**: 새로운 데이터가 활발히 입력되는 단계
- **Warm Phase**: 검색은 가능하지만 입력이 줄어든 단계  
- **Cold Phase**: 가끔씩만 검색되는 단계
- **Delete Phase**: 설정된 기간 후 자동 삭제

### 🎯 ILM의 장점

1. **자동화된 인덱스 관리**: 수동으로 오래된 인덱스를 삭제할 필요 없음
2. **스토리지 비용 최적화**: 오래된 데이터를 자동으로 정리
3. **성능 향상**: 핫 데이터와 콜드 데이터를 분리하여 검색 성능 향상
4. **운영 부담 감소**: 관리자의 개입 없이 자동으로 인덱스 롤오버 수행

### 📊 Jaeger에서 ILM 사용 예시

```yaml
# ILM 정책 예시
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_age": "3m",        # 3분마다 롤오버
            "max_size": "100mb"     # 100MB마다 롤오버
          }
        }
      },
      "delete": {
        "min_age": "5m",           # 5분 후 삭제
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

---

## Jaeger 버전별 테스트 결과

### ✅ Jaeger 2.6.0 - 완벽한 ILM 지원

```yaml
# 설정
elasticsearch:
  use_aliases: true
  use_ilm: true
  # create_index_templates 설정 없음 (기본값 사용)
```

**결과:**
- ✅ 정상 시작
- ✅ 스팬 데이터 Elasticsearch에 기록
- ✅ 모든 서비스가 Jaeger UI에 표시
- ✅ **완벽한 ILM 지원**: 모든 인덱스(span, service, dependencies)에 ILM 정책 적용

### ❌ Jaeger 2.7.0 - 실패

```bash
# 오류 발생
2025/09/06 12:55:28 invalid configuration: extensions::jaeger_storage::backends::es::elasticsearch: 
when UseILM is set true, CreateIndexTemplates must be set to false and index templates 
must be created by init process of es-rollover app
Error: invalid configuration: extensions::jaeger_storage::backends::es::elasticsearch: 
when UseILM is set true, CreateIndexTemplates must be set to false and index templates 
must be created by init process of es-rollover app
```

**오류 로그 상세 분석:**

| 구분 | 설명 |
|------|------|
| **오류 위치** | `extensions::jaeger_storage::backends::es::elasticsearch` |
| **검증 규칙** | `UseILM = true` 일 때 `CreateIndexTemplates = false` 필수 |
| **원인** | OpenTelemetry Collector는 `CreateIndexTemplates` 설정 제어 불가 |
| **해결 조건** | 인덱스 템플릿은 `es-rollover` 앱의 초기화 과정에서 생성되어야 함 |

**결과:**
- ❌ 시작 실패 (무한 재시작 루프)
- ❌ OpenTelemetry Collector 설정에서 `create_index_templates: false` 옵션 지원 안 함
- ❌ 구조적 제약으로 해결 불가능

### ❌ Jaeger 2.8.0, 2.9.0, 2.10.0 - 동일한 문제

모든 2.7.0 이상 버전에서 **동일한 검증 오류** 발생:

```bash
Error: invalid configuration: extensions::jaeger_storage::backends::es::elasticsearch: 
when UseILM is set true, CreateIndexTemplates must be set to false
```

---

## 🏗️ Jaeger 배포 방식 이해하기

### 📝 **핵심 개념: 동일한 Jaeger, 다른 설정 방법**

**중요한 오해 정정**: Jaeger에는 두 가지 설정 방식이 있지만, **동일한 Jaeger 바이너리**를 사용합니다!

```bash
# 컨테이너 내부에서 실제 실행되는 것
$ docker exec collector ps aux
PID   USER     TIME  COMMAND
    1 10001     0:00 /cmd/jaeger/jaeger-linux --config /jaeger/config.yaml
```

#### **🔍 차이점은 단순히 "설정 전달 방식"**

| 구분 | OpenTelemetry 방식 (현재) | CLI 플래그 방식 |
|------|---------------------------|----------------|
| **실행 바이너리** | `jaeger-linux` | `jaeger-linux` |
| **Docker 이미지** | `jaegertracing/jaeger:2.6.0` | `jaegertracing/jaeger:2.6.0` |
| **설정 전달** | `--config /jaeger/config.yaml` | `--es.use-ilm=true --es.create...` |
| **create_index_templates 제어** | ❌ 불가능 | ✅ 가능 |
| **버전 호환성** | ❌ (2.7.0+에서 제한) | ✅ (2.6.0까지만) |

#### **1️⃣ OpenTelemetry 설정 방식** (현재 사용 중)

```yaml
# config-elasticsearch.yaml
extensions:
  jaeger_storage:
    backends:
      es:
        elasticsearch:
          server_urls: ["http://elasticsearch:9200"]
          use_ilm: true          # ✅ 이 설정만 가능
          use_aliases: true      # ✅ 이 설정만 가능
          # ❌ create_index_templates 옵션 없음!
```

**제약사항**: OpenTelemetry 표준에서 지원하는 설정만 사용 가능

#### **2️⃣ CLI 플래그 방식** (전통적)

```dockerfile
FROM jaegertracing/jaeger:2.6.0
CMD ["./jaeger-collector", \
     "--es.server-urls=http://elasticsearch:9200", \
     "--es.use-ilm=true", \
     "--es.create-index-templates=false"]  # ✅ 이 옵션 사용 가능!
```

**장점**: 모든 Jaeger 전용 옵션 접근 가능

### 🚨 **중요한 변화: Jaeger 2.10.0의 CLI 플래그 완전 제거**

```bash
# Jaeger 2.10.0에서 시도한 결과
$ docker run --rm jaegertracing/jaeger:2.10.0 ./jaeger-collector --help
Error: unknown command "./jaeger-collector" for "jaeger"
```

**Jaeger 팀의 공식 변경 이유:**
- CLI와 YAML 두 방식 유지의 복잡성 해결
- OpenTelemetry 표준 준수
- 단일 바이너리 + 설정 파일로 단순화

**변경 타임라인:**
- **Jaeger v1 (2017~2021)**: CLI 플래그 방식
- **Jaeger v2 (2022~)**: YAML 설정 중심
- **Jaeger 2.10.0 (2025)**: CLI 플래그 완전 제거

---

## 문제 원인 심층 분석

### 🔍 핵심 원인

**Jaeger 2.7.0부터 도입된 엄격한 ILM 검증 로직**이 문제의 근본 원인입니다.

### 📝 **오류 메시지 완전 분석**

```
invalid configuration: extensions::jaeger_storage::backends::es::elasticsearch: 
when UseILM is set true, CreateIndexTemplates must be set to false and index templates 
must be created by init process of es-rollover app
```

#### **오류 메시지 구성 요소별 설명:**

| 구성 요소 | 의미 | 설명 |
|-----------|------|------|
| `invalid configuration` | 설정 유효성 검사 실패 | Jaeger가 시작 시 설정을 검증하는 과정에서 발견된 오류 |
| `extensions::jaeger_storage::backends::es::elasticsearch` | 오류 발생 위치 | OpenTelemetry Collector의 Jaeger Storage Extension 내부 |
| `UseILM is set true` | ILM 활성화 상태 | `use_ilm: true` 설정이 감지됨 |
| `CreateIndexTemplates must be set to false` | 필수 조건 | ILM 사용 시 반드시 `create_index_templates: false` 설정 필요 |
| `index templates must be created by init process` | 템플릿 생성 주체 | 인덱스 템플릿은 Jaeger가 아닌 초기화 프로세스에서 생성해야 함 |
| `es-rollover app` | 초기화 담당 앱 | `jaeger-es-rollover` 도구가 템플릿 생성을 담당해야 함 |

#### **🎯 오류의 핵심 의미:**

1. **설정 충돌**: ILM과 인덱스 템플릿 자동 생성을 동시에 사용할 수 없음
2. **역할 분리**: Jaeger는 데이터 저장, `es-rollover`는 인덱스 관리 담당
3. **초기화 순서**: ILM 사용 시 인덱스 템플릿이 먼저 생성되어 있어야 함

#### **🚫 왜 이 검증이 추가되었는가?**

**ILM과 자동 템플릿 생성의 충돌 방지:**
- ILM 정책이 적용된 템플릿이 필요
- Jaeger가 일반 템플릿을 생성하면 ILM 설정이 누락될 수 있음
- `es-rollover` 도구는 ILM 정책이 포함된 올바른 템플릿을 생성

### 📊 **성공/실패 로그 비교**

#### ✅ **Jaeger 2.6.0 정상 시작 로그:**
```
2025-09-06T13:49:16.952Z	info	service@v0.125.0/service.go:289	Everything is ready. Begin running and processing data.
2025-09-06T13:49:16.952Z	info	otlpreceiver@v0.125.0/otlp.go:116	Starting GRPC server
2025-09-06T13:49:16.952Z	info	otlpreceiver@v0.125.0/otlp.go:173	Starting HTTP server
```
**특징**: 에러 없이 정상적으로 GRPC/HTTP 서버 시작

#### ❌ **Jaeger 2.7.0+ 실패 로그:**
```
2025/09/06 13:44:52 invalid configuration: extensions::jaeger_storage::backends::es::elasticsearch: 
when UseILM is set true, CreateIndexTemplates must be set to false and index templates 
must be created by init process of es-rollover app
Error: invalid configuration: extensions::jaeger_storage::backends::es::elasticsearch: 
when UseILM is set true, CreateIndexTemplates must be set to false and index templates 
must be created by init process of es-rollover app
```
**특징**: 설정 검증 단계에서 즉시 실패, 무한 재시작 루프

#### 1. **새로운 검증 규칙**

```go
// Jaeger 2.7.0+ 검증 로직 (추정)
if config.UseILM && config.CreateIndexTemplates {
    return errors.New("when UseILM is set true, CreateIndexTemplates must be set to false")
}
```

#### 2. **OpenTelemetry Collector 제약사항**

OpenTelemetry Collector의 Jaeger Storage Extension은 다음 필드를 **지원하지 않습니다**:

```yaml
# ❌ 지원되지 않는 설정
elasticsearch:
  create_index_templates: false    # 이 필드가 존재하지 않음
  create-index-templates: false    # 이것도 동작하지 않음
```

#### 3. **Native Jaeger CLI vs OpenTelemetry Collector**

| 구분 | Native Jaeger CLI | OpenTelemetry Collector |
|------|-------------------|-------------------------|
| ILM 지원 | ✅ 완전 지원 | ⚠️ 제한적 지원 |
| `--es.create-index-templates=false` | ✅ 지원 | ❌ 미지원 |
| 설정 유연성 | ✅ 높음 | ❌ 제한적 |
| Jaeger 2.7.0+ 호환성 | ✅ 호환 | ❌ 비호환 |

### 📋 변경사항 타임라인

| 버전 | 변경사항 | ILM 상태 |
|------|----------|----------|
| **~2.6.0** | ILM 기본 지원, 검증 로직 없음 | ✅ 작동 (부분적) |
| **2.7.0+** | 엄격한 ILM 검증 로직 도입 | ❌ OpenTelemetry에서 실패 |


---

## 실제 검증 결과

### 🧪 Jaeger 2.6.0 ILM 동작 분석

실제로 Elasticsearch에서 ILM 적용 상태를 확인한 결과:

```bash
# ILM 정책이 적용된 인덱스 확인
$ curl -s "http://localhost:9200/jaeger-prod-*/_settings" | jq '...'
```

**결과:**
```json
[
  {
    "index": "jaeger-prod-jaeger-dependencies-000001",
    "lifecycle_name": "jaeger-ilm-policy",           # ✅ ILM 적용됨
    "rollover_alias": "jaeger-prod-jaeger-dependencies-write"
  },
  {
    "index": "jaeger-prod-jaeger-service-000001",
    "lifecycle_name": "jaeger-ilm-policy",           # ✅ ILM 적용됨
    "rollover_alias": "jaeger-prod-jaeger-service-write"
  },
  {
    "index": "jaeger-prod-jaeger-span-000001", 
    "lifecycle_name": "jaeger-ilm-policy",           # ✅ ILM 적용됨
    "rollover_alias": "jaeger-prod-jaeger-span-write"
  }
]
```

### 📊 중요한 발견사항

**Jaeger 2.6.0에서 ILM이 완벽하게 작동합니다!**

- ✅ `dependencies` 인덱스: ILM 정책 적용됨
- ✅ `service` 인덱스: ILM 정책 적용됨  
- ✅ `span` 인덱스: ILM 정책 적용됨

**ILM 정책 사용 현황:**
```json
{
  "indices": [
    "jaeger-prod-jaeger-dependencies-000001",
    "jaeger-prod-jaeger-service-000001", 
    "jaeger-prod-jaeger-span-000001"
  ],
  "composable_templates": [
    "jaeger-prod-jaeger-service",
    "jaeger-prod-jaeger-span", 
    "jaeger-prod-jaeger-dependencies"
  ]
}
```

---

## 🔍 ILM 지원 상태 확인 방법

### 📋 ILM 정책 및 인덱스 상태 확인 명령어

#### 1. **ILM 정책 존재 여부 확인**
```bash
# ILM 정책 목록 조회
curl -s "http://localhost:9200/_ilm/policy" | jq 'keys[]'

# 특정 정책 상세 조회
curl -s "http://localhost:9200/_ilm/policy/jaeger-ilm-policy" | jq
```

#### 2. **인덱스별 ILM 적용 상태 확인**
```bash
# 모든 Jaeger 인덱스의 ILM 설정 확인
curl -s "http://localhost:9200/jaeger-prod-*/_settings" | \
jq 'to_entries | map({
  index: .key, 
  lifecycle_name: .value.settings.index.lifecycle.name,
  rollover_alias: .value.settings.index.lifecycle.rollover_alias
}) | sort_by(.index)'
```

#### 3. **ILM 정책 사용 현황 확인**
```bash
# 정책을 사용하는 인덱스 목록
curl -s "http://localhost:9200/_ilm/policy/jaeger-ilm-policy" | \
jq '.["jaeger-ilm-policy"].in_use_by'
```

#### 4. **인덱스 별칭(Alias) 확인**
```bash
# Jaeger 관련 별칭 목록
curl -s "http://localhost:9200/_cat/aliases/jaeger-prod-*"

# 별칭 상세 정보
curl -s "http://localhost:9200/_aliases" | jq 'to_entries | 
map(select(.key | startswith("jaeger-prod"))) | 
from_entries'
```

#### 5. **인덱스 상태 및 크기 확인**
```bash
# Jaeger 인덱스 상태
curl -s "http://localhost:9200/_cat/indices/jaeger-prod-*?v&s=index"

# ILM 단계별 인덱스 상태
curl -s "http://localhost:9200/_ilm/explain/jaeger-prod-*" | jq
```

### 📊 **정상 작동 시 예상 출력**

#### ✅ **ILM이 올바르게 적용된 경우:**

```json
// 인덱스 ILM 설정
[
  {
    "index": "jaeger-prod-jaeger-dependencies-000001",
    "lifecycle_name": "jaeger-ilm-policy",
    "rollover_alias": "jaeger-prod-jaeger-dependencies-write"
  },
  {
    "index": "jaeger-prod-jaeger-service-000001", 
    "lifecycle_name": "jaeger-ilm-policy",
    "rollover_alias": "jaeger-prod-jaeger-service-write"
  },
  {
    "index": "jaeger-prod-jaeger-span-000001",
    "lifecycle_name": "jaeger-ilm-policy", 
    "rollover_alias": "jaeger-prod-jaeger-span-write"
  }
]

// ILM 정책 사용 현황
{
  "indices": [
    "jaeger-prod-jaeger-dependencies-000001",
    "jaeger-prod-jaeger-service-000001",
    "jaeger-prod-jaeger-span-000001"
  ],
  "composable_templates": [
    "jaeger-prod-jaeger-service",
    "jaeger-prod-jaeger-span", 
    "jaeger-prod-jaeger-dependencies"
  ]
}

// 별칭 상태
jaeger-prod-jaeger-dependencies-read  jaeger-prod-jaeger-dependencies-000001
jaeger-prod-jaeger-dependencies-write jaeger-prod-jaeger-dependencies-000001 (is_write_index=true)
jaeger-prod-jaeger-service-read       jaeger-prod-jaeger-service-000001
jaeger-prod-jaeger-service-write      jaeger-prod-jaeger-service-000001      (is_write_index=true)
jaeger-prod-jaeger-span-read          jaeger-prod-jaeger-span-000001
jaeger-prod-jaeger-span-write         jaeger-prod-jaeger-span-000001         (is_write_index=true)
```

#### ❌ **ILM이 적용되지 않은 경우:**

```json
// lifecycle_name이 null이거나 누락
[
  {
    "index": "jaeger-prod-jaeger-service-2025-09-06",
    "lifecycle_name": null,
    "rollover_alias": null
  }
]
```

### 🛠️ **문제 해결을 위한 디버깅 명령어**

#### ILM 정책 재생성
```bash
# ILM 정책 생성
curl -X PUT "localhost:9200/_ilm/policy/jaeger-ilm-policy" \
-H 'Content-Type: application/json' -d'
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "100mb",
            "max_age": "3m"
          }
        }
      },
      "delete": {
        "min_age": "5m",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}'
```

#### 인덱스 템플릿 확인
```bash
# Jaeger 인덱스 템플릿 목록
curl -s "http://localhost:9200/_index_template/jaeger-prod-*" | jq 'keys[]'

# 특정 템플릿 상세 조회  
curl -s "http://localhost:9200/_index_template/jaeger-prod-jaeger-span" | jq
```

---

## 해결 방안 및 권장사항

### 🎯 권장 해결책

#### 1. ~~**Native Jaeger CLI로 마이그레이션**~~ ❌ **불가능**

**❌ 2025년 현재 불가능한 이유:**
```dockerfile
# ❌ 더 이상 작동하지 않음 (2.10.0+)
FROM jaegertracing/jaeger:2.10.0
CMD ["./jaeger-collector", "--es.create-index-templates=false"]

# 결과: Error: unknown command "./jaeger-collector" for "jaeger"
```

**결론**: **Jaeger 2.10.0+에서는 CLI 플래그가 완전히 제거**되어 이 방법을 사용할 수 없습니다.

#### 1. **Jaeger 2.6.0 유지** ✅ **현실적 최선책**

**완벽하게 작동하는 2.6.0을 유지하여 ILM의 모든 기능 활용:**

```yaml
# docker-compose.yml (권장 구성)
collector:
  build:
    context: ./jaeger/collector
  image: jaegertracing/jaeger:2.6.0  # ILM 완벽 지원 마지막 버전
```

**장점:**
- ✅ **완벽한 ILM 지원** (모든 인덱스에 적용)
- ✅ **안정성 보장** (검증된 구성)
- ✅ **설정 복잡도 낮음**
- ✅ **OpenTelemetry 호환** (YAML 설정 지원)

#### 3. **수동 인덱스 관리** (대안)

ILM 대신 cron job이나 스크립트를 통한 수동 인덱스 정리:

```bash
#!/bin/bash
# 7일 이상 된 Jaeger 인덱스 삭제
curl -X DELETE "localhost:9200/jaeger-*-$(date -d '7 days ago' +%Y-%m-%d)"
```

### ⚠️ 주의사항

1. **Production 환경에서는 Native CLI 사용을 강력히 권장**
2. **OpenTelemetry Collector 사용 시 2.6.0 이하 버전만 사용**
3. **ILM 설정 전 반드시 테스트 환경에서 검증**

---

## 참고 자료

### 📚 공식 문서

- [Jaeger Elasticsearch Storage Configuration](https://www.jaegertracing.io/docs/2.10/storage/elasticsearch/)
- [Elasticsearch ILM Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-lifecycle-management.html)
- [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/)

### 🔗 관련 GitHub 이슈 및 PR

- [Add support for Elasticsearch ILM Policies - PR #2454](https://github.com/jaegertracing/jaeger/pull/2454)
- [Support for Elasticsearch ILM policies - Issue #2048](https://github.com/jaegertracing/jaeger/issues/2048)
- [Jaeger and elasticsearch ILM - Discussion #3003](https://github.com/jaegertracing/jaeger/discussions/3003)
- [Elasticsearch 8.x support - Issue #3571](https://github.com/jaegertracing/jaeger/issues/3571)

### 🛠️ 실용적 가이드

- [Managing Jaeger and Elasticsearch ILM - dimmaski](https://dimmaski.com/ilm-elasticsearch-jaeger/)
- [Speeding up Jaeger on Elasticsearch](https://karlstoney.com/speeding-up-jaeger-on-elasticsearch/)
- [Jaeger Index Rollover with ILM - GitHub](https://github.com/bhiravabhatla/jaeger-index-rollover-with-ilm)

---

## 💡 결론

### 🎯 **최종 권장사항**

**Jaeger와 Elasticsearch ILM을 함께 사용하려면:**

1. ✅ **Jaeger 2.6.0 고정 사용** - 현재 유일한 안정적 해결책
2. ❌ ~~최신 버전 업그레이드~~ - 2.7.0+에서 비호환
3. ⚠️ **수동 인덱스 관리** - 차선책 (비권장)

### 📋 **버전별 호환성 요약**

| Jaeger 버전 | ILM 지원 | 호환성 | 권장도 |
|-------------|----------|--------|--------|
| **2.6.0** | ✅ 완벽 지원 | ✅ OpenTelemetry | 🟢 **강력 권장** |
| **2.7.0~2.9.0** | ❌ 검증 실패 | ❌ 비호환 | 🔴 사용 불가 |
| **2.10.0+** | ❓ 미검증 | ❌ CLI 제거 | 🔴 해결 방법 없음 |

### 🚨 **핵심 포인트**

- **2.6.0**: OpenTelemetry Collector에서 ILM 완벽 지원하는 마지막 버전
- **2.7.0+**: 엄격한 ILM 검증 로직으로 OpenTelemetry 비호환  
- **2.10.0+**: CLI 플래그 완전 제거로 우회 방법 없음
- **결론**: **Jaeger 2.6.0이 현실적으로 유일한 선택지**

---

---

> **이 문서는 2025년 9월 6일 기준으로 Jaeger 2.6.0~2.10.0 버전에서 실제 테스트한 결과를 바탕으로 작성되었습니다.**  
> **향후 Jaeger 팀의 ILM 지원 개선 사항을 지속적으로 모니터링할 예정입니다.**