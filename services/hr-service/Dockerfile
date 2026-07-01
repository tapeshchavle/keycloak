FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY mvnw* ./
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw 2>/dev/null || true
RUN ./mvnw package -DskipTests --no-transfer-progress 2>/dev/null || mvn package -DskipTests --no-transfer-progress
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
