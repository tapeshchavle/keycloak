# ============================================================
# Dockerfile — hr-service
# eclipse-temurin:17-jdk  → ARM64 + AMD64 compatible
# Works on Apple Silicon M1/M2/M3 and Intel/AMD x86_64
# ============================================================

FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw
RUN ./mvnw package -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=builder /workspace/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
