FROM openjdk:17-jdk-slim
COPY /build/libs/*.jar flight.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar flight.jar"]