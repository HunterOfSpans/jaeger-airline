# 문서 가이드

이 디렉토리는 Jaeger 항공 예약 시스템의 기술 문서를 포함합니다.

---

## 문서 구조

```
docs/
├── README.md                 # 이 파일 (문서 인덱스)
├── glossary.md               # 용어집
├── getting-started/          # 시작하기
│   └── jaeger-otel.md        # Spring Boot + OTel + Jaeger 가이드
├── architecture/             # 아키텍처 설계
│   ├── otel-sdk-implementation.md  # OTel SDK 구현 가이드
│   ├── library-sharing-guide.md    # 공통 라이브러리 공유 가이드
│   └── Jaeger-CQRS-Architecture-Guide.md  # Jaeger CQRS 아키텍처
├── guides/                   # 실습 가이드
│   ├── security-guide.md           # 보안 가이드
│   ├── OpenFeign-Distributed-Tracing-Guide.md   # OpenFeign 추적 가이드
│   └── Kafka-Distributed-Tracing-Complete-Guide.md  # Kafka 추적 가이드
├── reference/                # 기술 참조
│   └── Jaeger-ILM-Compatibility-Analysis.md  # ILM 호환성 분석
└── troubleshooting/          # 문제 해결
    └── elasticsearch-mapping-conflict.md  # ES 매핑 충돌 해결
```

---

## 빠른 시작

1. **처음 시작하는 경우**: [Spring Boot + OTel + Jaeger 가이드](getting-started/jaeger-otel.md)
2. **용어가 헷갈리는 경우**: [용어집](glossary.md)

---

## 문서 카테고리

### 시작하기 (Getting Started)

| 문서 | 설명 | 난이도 |
|------|------|--------|
| [jaeger-otel.md](getting-started/jaeger-otel.md) | Spring Boot 3 + OTel + Jaeger v2 통합 가이드 | 입문 |

### 아키텍처 (Architecture)

| 문서 | 설명 | 난이도 |
|------|------|--------|
| [otel-sdk-implementation.md](architecture/otel-sdk-implementation.md) | OTel SDK 기반 분산 추적 구현 | 중급 |
| [library-sharing-guide.md](architecture/library-sharing-guide.md) | Gradle Composite Build로 라이브러리 공유 | 중급 |
| [Jaeger-CQRS-Architecture-Guide.md](architecture/Jaeger-CQRS-Architecture-Guide.md) | Jaeger의 CQRS 아키텍처 분석 | 고급 |

### 실습 가이드 (Guides)

| 문서 | 설명 | 난이도 |
|------|------|--------|
| [security-guide.md](guides/security-guide.md) | 시스템 보안 구성 가이드 | 중급 |
| [OpenFeign-Distributed-Tracing-Guide.md](guides/OpenFeign-Distributed-Tracing-Guide.md) | OpenFeign 자동 분산 추적 | 중급 |
| [Kafka-Distributed-Tracing-Complete-Guide.md](guides/Kafka-Distributed-Tracing-Complete-Guide.md) | Kafka 수동 분산 추적 구현 | 중급 |

### 기술 참조 (Reference)

| 문서 | 설명 | 난이도 |
|------|------|--------|
| [Jaeger-ILM-Compatibility-Analysis.md](reference/Jaeger-ILM-Compatibility-Analysis.md) | Jaeger 버전별 ILM 호환성 분석 | 고급 |

### 문제 해결 (Troubleshooting)

| 문서 | 설명 | 난이도 |
|------|------|--------|
| [elasticsearch-mapping-conflict.md](troubleshooting/elasticsearch-mapping-conflict.md) | ES 인덱스 매핑 충돌 해결 | 중급 |

---

## 문서 작성 규칙

모든 문서는 다음 형식을 따릅니다:

```markdown
# 문서 제목

> **작성일**: YYYY-MM-DD | **버전**: 관련 버전들 | **난이도**: 입문/중급/고급

---

## 목차

- [섹션 1](#섹션-1)
- [섹션 2](#섹션-2)
...

---

## 섹션 1
...
```

---

## 기여하기

문서에 오류가 있거나 개선이 필요한 경우:

1. 이슈를 등록하거나
2. Pull Request를 생성해주세요

---

## 버전 정보

| 구성 요소 | 버전 |
|-----------|------|
| Jaeger | 2.6.0 |
| Spring Boot | 3.3.5 |
| OpenTelemetry | 2.11.0 |
| Elasticsearch | 8.x |
| Kafka | 3.x |
