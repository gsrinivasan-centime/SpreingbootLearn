# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/book-service-1.0.0.jar app.jar

# Expose the port
EXPOSE 8081

# Set environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8081
ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/bookstore_books
ENV SPRING_DATASOURCE_USERNAME=bookstore_user
ENV SPRING_DATASOURCE_PASSWORD=bookstore_pass
ENV SPRING_REDIS_HOST=redis
ENV SPRING_REDIS_PORT=6379
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
