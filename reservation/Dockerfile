FROM gradle:8.5-jdk17 AS builder
COPY --chown=gradle:gradle . /reservation
WORKDIR /reservation
RUN gradle build -x test

FROM openjdk:17-jdk-slim
COPY --from=builder /reservation/build/libs/*.jar reservation.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/reservation.jar"]