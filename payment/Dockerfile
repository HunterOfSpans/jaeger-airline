FROM openjdk:17-jdk-slim
COPY /build/libs/*.jar payment.jar
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar payment.jar"]