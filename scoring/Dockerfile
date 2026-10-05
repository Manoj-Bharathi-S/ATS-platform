# Stage 1: Build the Scoring Engine using Maven wrapper
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and configuration files first for optimal caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Give execution permission to the Maven wrapper
RUN chmod +x mvnw

# Download dependencies (cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the fat JAR
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the Scoring Engine on a lightweight JRE image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render assigns a dynamic port via environment variable; expose 8081 as a fallback
EXPOSE 8081

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]