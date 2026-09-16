# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first to cache the Maven dependency layer.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application.
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight runtime
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Run as a non-root user.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy the generated JAR.
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]