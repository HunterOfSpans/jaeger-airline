# 공통 라이브러리 공유 및 배포 가이드

> **작성일**: 2026-01-04 | **버전**: Gradle 8.x, Spring Boot 3.3.5 | **난이도**: 중급

---

## 목차

- [1. 현재 구성: Gradle Composite Build](#1-현재-구성-gradle-composite-build)
- [2. Nexus/Artifactory 배포 방법](#2-nexusartifactory-배포-방법)
- [3. 버전 관리 전략](#3-버전-관리-전략)
- [4. 로컬 개발 팁](#4-로컬-개발-팁)
- [5. 트러블슈팅](#5-트러블슈팅)
- [6. 참고 자료](#6-참고-자료)

---

## 1. 현재 구성: Gradle Composite Build

### 1.1 왜 Composite Build를 선택했는가?

| 방식 | 장점 | 단점 |
|------|------|------|
| **Composite Build** | 즉시 반영, 설정 간단, IDE 지원 | 같은 저장소 내 필요 |
| **publishToMavenLocal** | 버전 관리 가능 | 매번 publish 필요 |
| **Nexus/Artifactory** | 완전 독립, 팀 공유 | 인프라 필요, 배포 파이프라인 |
| **Git Submodule** | 독립 저장소 | 관리 복잡 |

**현재 상황에 적합한 이유:**
- 단일 저장소(monorepo) 내 MSA 구성
- 개발 중 빠른 피드백 필요
- 별도 인프라 없이 즉시 사용 가능
- IDE에서 소스 코드 탐색/디버깅 가능

### 1.2 프로젝트 구조

```
jaeger-airline/
├── common/
│   └── kafka-tracing/          # 공통 라이브러리
│       ├── build.gradle.kts
│       ├── settings.gradle.kts
│       └── src/
│           └── main/kotlin/com/airline/tracing/
│               ├── annotation/KafkaOtelTrace.kt
│               ├── aspect/KafkaTracingAspect.kt
│               └── config/KafkaTracingAutoConfiguration.kt
├── flight/                      # Java 서비스
│   ├── settings.gradle         # includeBuild('../common/kafka-tracing')
│   └── build.gradle            # implementation 'com.airline:kafka-tracing'
├── payment/                     # Kotlin 서비스
│   ├── settings.gradle.kts     # includeBuild("../common/kafka-tracing")
│   └── build.gradle.kts        # implementation("com.airline:kafka-tracing")
├── ticket/
└── reservation/
```

### 1.3 각 서비스 설정

**settings.gradle.kts (Kotlin 서비스):**
```kotlin
rootProject.name = "ticket"

// Composite Build: 공통 라이브러리 포함
includeBuild("../common/kafka-tracing")
```

**settings.gradle (Java 서비스):**
```groovy
rootProject.name = 'flight'

// Composite Build: 공통 라이브러리 포함
includeBuild('../common/kafka-tracing')
```

**build.gradle.kts:**
```kotlin
dependencies {
    // Common Libraries (Composite Build)
    implementation("com.airline:kafka-tracing")

    // 기타 의존성...
}
```

### 1.4 Composite Build 동작 원리

1. Gradle이 `settings.gradle.kts`에서 `includeBuild` 발견
2. 지정된 경로의 프로젝트를 "참여 빌드"로 포함
3. `com.airline:kafka-tracing` 의존성 요청 시:
   - Maven Central 대신 로컬 `common/kafka-tracing` 모듈 사용
   - 소스 변경 시 자동으로 재빌드
4. IDE에서 소스 코드 직접 탐색 가능

---

## 2. Nexus/Artifactory 배포 방법

실제 운영 환경이나 팀 간 공유가 필요할 때 사용.

### 2.1 build.gradle.kts 수정

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.7"
    `maven-publish`
}

group = "com.airline"
version = "1.0.0"  // 버전 관리

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Kafka Tracing Common")
                description.set("Common Kafka tracing library with OpenTelemetry support")
                url.set("https://github.com/your-org/jaeger-airline")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("your-team")
                        name.set("Your Team")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "nexus"

            // 스냅샷 vs 릴리즈 구분
            val releasesRepoUrl = uri("https://nexus.your-company.com/repository/maven-releases/")
            val snapshotsRepoUrl = uri("https://nexus.your-company.com/repository/maven-snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = System.getenv("NEXUS_USERNAME") ?: project.findProperty("nexusUsername") as String?
                password = System.getenv("NEXUS_PASSWORD") ?: project.findProperty("nexusPassword") as String?
            }
        }
    }
}
```

### 2.2 배포 명령어

```bash
# 로컬 Maven 저장소에 배포 (테스트용)
./gradlew publishToMavenLocal

# Nexus에 배포
./gradlew publish

# 특정 버전으로 배포
./gradlew publish -Pversion=1.0.1
```

### 2.3 CI/CD 파이프라인 예시 (GitHub Actions)

```yaml
# .github/workflows/publish-library.yml
name: Publish Library

on:
  push:
    tags:
      - 'kafka-tracing-v*'

jobs:
  publish:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Extract version from tag
        id: version
        run: echo "VERSION=${GITHUB_REF#refs/tags/kafka-tracing-v}" >> $GITHUB_OUTPUT

      - name: Publish to Nexus
        working-directory: common/kafka-tracing
        run: ./gradlew publish -Pversion=${{ steps.version.outputs.VERSION }}
        env:
          NEXUS_USERNAME: ${{ secrets.NEXUS_USERNAME }}
          NEXUS_PASSWORD: ${{ secrets.NEXUS_PASSWORD }}
```

### 2.4 서비스에서 Nexus 라이브러리 사용

Composite Build 대신 Nexus에서 가져오도록 변경:

**settings.gradle.kts:**
```kotlin
rootProject.name = "ticket"

// Composite Build 제거 (또는 주석 처리)
// includeBuild("../common/kafka-tracing")
```

**build.gradle.kts:**
```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://nexus.your-company.com/repository/maven-releases/")
        credentials {
            username = System.getenv("NEXUS_USERNAME") ?: ""
            password = System.getenv("NEXUS_PASSWORD") ?: ""
        }
    }
}

dependencies {
    implementation("com.airline:kafka-tracing:1.0.0")  // 버전 명시
}
```

---

## 3. 버전 관리 전략

### 3.1 Semantic Versioning

```
MAJOR.MINOR.PATCH
  │     │     │
  │     │     └── 버그 수정 (하위 호환)
  │     └──────── 기능 추가 (하위 호환)
  └────────────── Breaking Changes
```

**예시:**
- `1.0.0` → `1.0.1`: 버그 수정
- `1.0.1` → `1.1.0`: 새 속성 추가 (recordBatchInfo 등)
- `1.1.0` → `2.0.0`: 어노테이션 구조 변경 (Breaking)

### 3.2 버전 업데이트 시나리오

**시나리오 1: 패치 (버그 수정)**
```bash
# 1.0.0 → 1.0.1
git tag kafka-tracing-v1.0.1
git push origin kafka-tracing-v1.0.1
```

**시나리오 2: 마이너 (기능 추가)**
```kotlin
// KafkaOtelTrace.kt에 새 속성 추가
annotation class KafkaOtelTrace(
    // 기존 속성들...
    val recordBatchInfo: Boolean = false  // 새로 추가
)
```
```bash
# 1.0.1 → 1.1.0
git tag kafka-tracing-v1.1.0
git push origin kafka-tracing-v1.1.0
```

---

## 4. 로컬 개발 팁

### 4.1 Composite Build vs Nexus 전환

개발 중에는 Composite Build, 배포 시에는 Nexus 버전 사용:

```kotlin
// settings.gradle.kts
val useLocalLibrary = System.getenv("USE_LOCAL_LIB")?.toBoolean() ?: true

if (useLocalLibrary) {
    includeBuild("../common/kafka-tracing")
}
```

```bash
# 로컬 라이브러리 사용 (기본)
./gradlew build

# Nexus 버전 사용
USE_LOCAL_LIB=false ./gradlew build
```

### 4.2 라이브러리 변경 후 빌드

Composite Build에서는 자동으로 반영되지만, 명시적으로 재빌드하려면:

```bash
# 공통 라이브러리만 빌드
cd common/kafka-tracing && ./gradlew build

# 또는 서비스 빌드 시 함께 빌드
cd ticket && ./gradlew build --include-build ../common/kafka-tracing
```

---

## 5. 트러블슈팅

### 5.1 "Could not resolve com.airline:kafka-tracing"

**원인:** Composite Build 설정 누락

**해결:**
```kotlin
// settings.gradle.kts에 추가
includeBuild("../common/kafka-tracing")
```

### 5.2 "Class not found: KafkaTracingAspect"

**원인:** Auto-configuration 미작동

**해결:**
1. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 확인
2. 또는 명시적 Bean 등록:
```kotlin
@Configuration
class ManualTracingConfig {
    @Bean
    fun kafkaTracingAspect(openTelemetry: OpenTelemetry) =
        KafkaTracingAspect(openTelemetry)
}
```

### 5.3 버전 충돌

**원인:** Composite Build와 Nexus 버전 혼재

**해결:** 한 가지 방식만 사용
```kotlin
// Composite Build 사용 시 버전 명시 불필요
implementation("com.airline:kafka-tracing")  // ✓

// Nexus 사용 시 버전 필수
implementation("com.airline:kafka-tracing:1.0.0")  // ✓
```

---

## 6. 참고 자료

- [Gradle Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html)
- [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Sonatype Nexus Repository](https://help.sonatype.com/repomanager3)
