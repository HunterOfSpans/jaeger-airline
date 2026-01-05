plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.airline"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

dependencyManagement{
	imports {
		mavenBom ("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.11.0")
		mavenBom ("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Common Libraries (Composite Build)
	implementation("com.airline:kafka-tracing")

	// kotlin
	implementation ("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation ("org.jetbrains.kotlin:kotlin-reflect")

	// spring
	implementation ("org.springframework.boot:spring-boot-starter-actuator")
	implementation ("org.springframework.boot:spring-boot-starter-web")
	implementation ("org.springframework.boot:spring-boot-starter-aop")
	implementation ("org.springframework.kafka:spring-kafka")

	// OpenFeign
	implementation ("org.springframework.cloud:spring-cloud-starter-openfeign")
	
	// Circuit Breaker
	implementation ("io.github.resilience4j:resilience4j-spring-boot3")

	// otel
	implementation ("io.micrometer:micrometer-tracing-bridge-otel")
	implementation ("io.github.openfeign:feign-micrometer:13.6")
	implementation ("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")

	// testing
	testRuntimeOnly ("org.junit.platform:junit-platform-launcher")
	testImplementation ("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation ("org.springframework.boot:spring-boot-starter-test")
	testImplementation ("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
