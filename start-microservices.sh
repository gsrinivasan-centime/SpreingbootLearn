#!/bin/bash

# ===================================================================
# SPRING BOOT MICROSERVICES DOCKER DEPLOYMENT
# ===================================================================
# 
# This script provides CONTAINERIZED deployment using Docker Compose
# 
# Containerized deployment advantages:
# ✅ Isolated environments
# ✅ No local infrastructure setup required  
# ✅ Consistent across development machines
# ✅ Easy scaling and management
# ✅ Production-ready setup
# ===================================================================

echo "🚀 Starting Spring Boot Microservices in Docker..."

# Build the microservices JARs first
echo "🔨 Building microservices..."
echo "📚 Building Book Service..."
cd book-service && mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ Failed to build book-service"
    exit 1
fi
cd ..

echo "👥 Building User Service..."
cd user-service && mvn clean package -DskipTests  
if [ $? -ne 0 ]; then
    echo "❌ Failed to build user-service"
    exit 1
fi
cd ..

# Start infrastructure services first
echo "🏗️ Starting infrastructure services..."
docker-compose up -d mysql redis zookeeper kafka redis-commander kafka-ui

# Wait for infrastructure to be healthy
echo "⏳ Waiting for infrastructure services to be ready..."
sleep 30

# Check infrastructure health
echo "🔍 Checking infrastructure health..."
docker-compose ps

# Build and start microservices
echo "📦 Building and starting microservices..."
docker-compose up -d --build book-service user-service

echo ""
echo "✅ All services are starting up..."
echo "📚 Book Service will be available at: http://localhost:8081/api/v1"
echo "👥 User Service will be available at: http://localhost:8082/api/v1"
echo ""
echo "🌐 Infrastructure Services:"
echo "  📊 Kafka UI: http://localhost:8080"
echo "  🗄️  Redis Commander: http://localhost:8090"
echo ""
echo "📋 Swagger Documentation:"
echo "  📚 Book Service: http://localhost:8081/api/v1/swagger-ui.html"
echo "  👥 User Service: http://localhost:8082/api/v1/swagger-ui.html"
echo ""
echo "📊 Health Checks:"
echo "  📚 Book Service: http://localhost:8081/api/v1/actuator/health"
echo "  👥 User Service: http://localhost:8082/api/v1/actuator/health"
echo ""
echo "🐳 To check container status:"
echo "  docker-compose ps"
echo ""
echo "📊 To view logs:"
echo "  docker-compose logs book-service"
echo "  docker-compose logs user-service"
echo ""
echo "🛑 To stop all services:"
echo "  docker-compose down"
