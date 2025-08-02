FROM gradle:8.5-jdk17 AS builder
COPY --chown=gradle:gradle . /flight
WORKDIR /flight
RUN gradle build -x test

FROM openjdk:17-jdk-slim
COPY --from=builder /flight/build/libs/*.jar flight.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/flight.jar"]