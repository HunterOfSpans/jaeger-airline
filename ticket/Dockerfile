FROM gradle:8.5-jdk17 AS builder
COPY --chown=gradle:gradle . /ticket
WORKDIR /ticket
RUN gradle build -x test

FROM openjdk:17-jdk-slim
COPY --from=builder /ticket/build/libs/*.jar ticket.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/ticket.jar"]