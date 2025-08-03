FROM gradle:8.5-jdk17 AS builder
COPY --chown=gradle:gradle . /payment
WORKDIR /payment
RUN gradle build -x test

FROM openjdk:17-jdk-slim
COPY --from=builder /payment/build/libs/*.jar payment.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/payment.jar"]