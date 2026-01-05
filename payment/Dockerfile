# Payment Service Dockerfile
# Kotlin 기반의 결제 처리 서비스
FROM eclipse-temurin:21-jre

# Gradle로 빌드된 JAR 파일을 컨테이너로 복사
COPY /build/libs/*.jar payment.jar

# 서비스 포트 8082 노출
EXPOSE 8082

# JAVA_OPTS 환경변수로 JVM 옵션 설정 가능
# 예: -Xmx384m -Xms256m (메모리 제한)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar payment.jar"]