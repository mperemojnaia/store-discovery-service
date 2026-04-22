# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -T 1C

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S -G appgroup -h /app -s /sbin/nologin appuser \
    && apk add --no-cache curl

COPY --from=build /app/target/store-locator.jar app.jar
RUN chown appuser:appgroup app.jar

ENV SERVER_PORT=8080
ENV DISTANCE_STRATEGY=haversine

EXPOSE 8080

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
