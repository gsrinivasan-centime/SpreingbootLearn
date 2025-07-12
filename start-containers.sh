#!/bin/bash

# Containerized Spring Boot Microservice Starter
echo "🐳 Starting Containerized Spring Boot Microservices..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    echo "❌ Docker Compose is not available. Please install Docker Compose."
    exit 1
fi

# Use docker compose (newer) or docker-compose (older)
COMPOSE_CMD="docker compose"
if ! docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
fi

echo "📦 Building services..."

# Build book-service if JAR doesn't exist
if [ ! -f "./book-service/target/book-service-1.0.0.jar" ]; then
    echo "🔨 Building book-service JAR..."
    cd book-service
    mvn clean package -DskipTests -Dspring-cloud-contract.verifier.failOnNoContracts=false
    cd ..
    
    if [ ! -f "./book-service/target/book-service-1.0.0.jar" ]; then
        echo "❌ Failed to build book-service JAR"
        exit 1
    fi
fi

# Note about user-service
echo "⚠️  user-service currently has compilation issues (Lombok). Skipping for now."

echo "🚀 Starting infrastructure services..."
$COMPOSE_CMD up -d mysql redis zookeeper kafka

echo "⏳ Waiting for infrastructure to be ready..."
sleep 15

echo "📊 Starting management tools..."
$COMPOSE_CMD up -d kafka-ui redis-commander

echo "🚀 Starting microservices..."
$COMPOSE_CMD up -d book-service

echo ""
echo "✅ Services started! Here are the available endpoints:"
echo ""
echo "📚 Book Service:"
echo "  🌐 API: http://localhost:8081/api/v1"
echo "  ❤️  Health: http://localhost:8081/api/v1/actuator/health"
echo ""
echo "🛠️  Management Tools:"
echo "  📊 Kafka UI: http://localhost:8080"
echo "  🗄️  Redis Commander: http://localhost:8090"
echo ""
echo "📊 Check service status:"
echo "  docker ps"
echo "  $COMPOSE_CMD logs -f book-service"
echo ""
echo "🛑 To stop all services:"
echo "  $COMPOSE_CMD down"
echo ""
echo "🗑️  To stop and remove volumes:"
echo "  $COMPOSE_CMD down -v"
