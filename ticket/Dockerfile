FROM openjdk:17-jdk-slim
COPY /build/libs/*.jar ticket.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ticket.jar"]