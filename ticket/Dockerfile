# Ticket Service Dockerfile
# Kotlin 기반의 항공권 발급 서비스
FROM eclipse-temurin:21-jre

# Gradle로 빌드된 JAR 파일을 컨테이너로 복사
COPY /build/libs/*.jar ticket.jar

# 서비스 포트 8081 노출
EXPOSE 8081

# JAVA_OPTS 환경변수로 JVM 옵션 설정 가능
# 예: -Xmx384m -Xms256m (메모리 제한)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ticket.jar"]