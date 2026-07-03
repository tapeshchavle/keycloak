# ============================================================
# Dockerfile — Keyclock API Gateway (Spring Boot)
# Uses eclipse-temurin:17-jdk (ARM64 + AMD64 compatible)
# Works on Apple Silicon M1/M2/M3 and Intel/AMD x86_64
# ============================================================

# Stage 1: Build
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw
RUN ./mvnw package -DskipTests --no-transfer-progress

# Stage 2: Runtime (smaller JRE-only image)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Security: Run as non-root user (Ubuntu syntax)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the fat JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
