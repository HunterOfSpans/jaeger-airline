FROM openjdk:17-jdk-slim
COPY /build/libs/*.jar reservation.jar
EXPOSE 8083
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar reservation.jar"]