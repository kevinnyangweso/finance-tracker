# ============================================
# Stage 1: Build the JAR
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the pom.xml first — this caches dependencies
COPY pom.xml .

# Download dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline

# Copy the source code
COPY src ./src

# Build the JAR (skip tests — they already ran in CI)
RUN mvn clean package -DskipTests

# ============================================
# Stage 2: Runtime (smaller image)
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port (Spring Boot reads PORT env var)
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]