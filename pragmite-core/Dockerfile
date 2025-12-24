# Multi-stage build for Pragmite
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Create runtime image
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Pragmite Team"
LABEL description="Pragmite - Java Code Quality Analysis Tool"
LABEL version="1.0.0"

# Install bash for better shell support
RUN apk add --no-cache bash

WORKDIR /pragmite

# Copy JAR from builder stage
COPY --from=builder /build/target/pragmite-core-1.0.0.jar /pragmite/pragmite.jar

# Create directories for volumes
RUN mkdir -p /pragmite/config \
    /pragmite/projects \
    /pragmite/reports \
    /pragmite/logs

# Configuration volume (for .pragmite.yaml)
VOLUME ["/pragmite/config"]

# Projects volume (source code to analyze)
VOLUME ["/pragmite/projects"]

# Reports volume (analysis outputs)
VOLUME ["/pragmite/reports"]

# Logs volume
VOLUME ["/pragmite/logs"]

# Set environment variables
ENV JAVA_OPTS="-Xmx2g -Xms512m"

# Entrypoint script
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /pragmite/pragmite.jar \"$@\"", "--"]

# Default command shows help
CMD ["--help"]

# Usage examples:
# docker build -t pragmite:1.0.0 .
# docker run -v $(pwd)/test-ecommerce:/pragmite/projects pragmite:1.0.0 /pragmite/projects
# docker run -v $(pwd):/pragmite/projects -v $(pwd)/reports:/pragmite/reports pragmite:1.0.0 /pragmite/projects -o /pragmite/reports/report.json
