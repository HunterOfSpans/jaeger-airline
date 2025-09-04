# Reservation Service Dockerfile
# Kotlin 기반의 예약 오케스트레이션 서비스
# Saga 패턴과 Circuit Breaker 구현
FROM openjdk:17-jdk-slim

# Gradle로 빌드된 JAR 파일을 컨테이너로 복사
COPY /build/libs/*.jar reservation.jar

# 서비스 포트 8083 노출 (메인 오케스트레이션 서비스)
EXPOSE 8083

# JAVA_OPTS 환경변수로 JVM 옵션 설정 가능
# 예: -Xmx384m -Xms256m (메모리 제한)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar reservation.jar"]