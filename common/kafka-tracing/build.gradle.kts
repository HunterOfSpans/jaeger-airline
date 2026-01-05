plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.7"
    `maven-publish`
}

group = "com.airline"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5")
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.11.0")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring Boot (provided - 사용하는 서비스에서 제공)
    compileOnly("org.springframework.boot:spring-boot-starter")
    compileOnly("org.springframework.boot:spring-boot-starter-aop")
    compileOnly("org.springframework.kafka:spring-kafka")

    // OpenTelemetry
    compileOnly("io.opentelemetry:opentelemetry-api")
    compileOnly("io.opentelemetry:opentelemetry-context")
    compileOnly("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")

    // Logging
    compileOnly("org.slf4j:slf4j-api")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// JAR 설정 (Spring Boot fat JAR 비활성화)
tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}

// Maven Local 배포 설정
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Kafka Tracing Common")
                description.set("Common Kafka tracing library with OpenTelemetry support")
            }
        }
    }
}
