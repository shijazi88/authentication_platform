# ============================================================================
# Sannad backend — Spring Boot on Java 21
#
# Multi-stage build:
#   1. builder  — Maven + JDK 21 compiles the app into target/*.jar
#   2. runtime  — Eclipse Temurin JRE 21 slim, just runs the jar
#
# Railway build context is the repo root. Listens on the PORT env var
# Railway injects (defaults to 8080).
# ============================================================================

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache Maven dependencies separately from source for faster rebuilds
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Extract the fat jar's layers so we can copy them in order (faster cold starts)
RUN mkdir -p /build/extracted && \
    java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /build/extracted

# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user for container hardening
RUN addgroup -S sannad && adduser -S sannad -G sannad
USER sannad

COPY --from=builder --chown=sannad:sannad /build/extracted/dependencies/ ./
COPY --from=builder --chown=sannad:sannad /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=sannad:sannad /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=sannad:sannad /build/extracted/application/ ./

# Railway injects PORT at runtime. application.yml reads it directly
# via server.port: ${PORT:8080}. No need for SERVER_PORT.
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", \
            "org.springframework.boot.loader.launch.JarLauncher"]
