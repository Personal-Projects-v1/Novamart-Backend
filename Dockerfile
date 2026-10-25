# Multi-stage build for Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for Docker build)
RUN ./mvnw clean package -DskipTests -B

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 spring && \
    adduser -D -u 1001 -G spring spring

# Set working directory
WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder /app/target/*.war app.war

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
