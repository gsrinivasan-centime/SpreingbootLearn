# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the JAR file (when it's built)
COPY target/user-service-1.0.0.jar app.jar

# Expose the port
EXPOSE 8082

# Set environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8082
ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3307/bookstore_users
ENV SPRING_DATASOURCE_USERNAME=bookstore_user
ENV SPRING_DATASOURCE_PASSWORD=bookstore_pass
ENV SPRING_REDIS_HOST=redis
ENV SPRING_REDIS_PORT=6379
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
