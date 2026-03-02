# ================================
# STAGE 1: Build Stage
# ================================
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# ================================
# STAGE 2: Runtime Stage
# ================================
FROM eclipse-temurin:17-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Set default profile to production
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

