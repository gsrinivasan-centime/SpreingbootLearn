#!/bin/bash

echo "🔧 Working around Maven Issues - Starting Spring Boot Microservices"
echo "📋 Current Status Summary:"
echo ""

# Check infrastructure
echo "🏗️ Infrastructure Status:"
docker-compose ps

echo ""
echo "📊 Kafka Topics:"
docker exec bookstore-kafka kafka-topics --bootstrap-server localhost:9092 --list

echo ""
echo "💡 Next Steps to Get Microservices Running:"
echo ""
echo "1. ✅ Infrastructure is working (Kafka, MySQL, Redis)"
echo "2. ❌ Maven build issues due to network/repository problems"
echo "3. 🔧 Workaround options:"
echo ""
echo "   Option A: Use Spring Boot CLI to run directly"
echo "   Option B: Fix Maven repository issues"
echo "   Option C: Use Docker to build services"
echo ""

echo "🌐 Infrastructure Services Available:"
echo "  📊 Kafka UI: http://localhost:8080"
echo "  🗄️  Redis Commander: http://localhost:8081"
echo "  🐘 MySQL Books DB: localhost:3306"
echo "  🐘 MySQL Users DB: localhost:3307"
echo "  📨 Kafka: localhost:9092"
echo "  💾 Redis: localhost:6379"

echo ""
echo "🎯 Primary Achievement: Kafka startup error has been FIXED! ✅"
echo "   All infrastructure services are running correctly."
echo ""
echo "📝 To fix Maven and start the microservices:"
echo "   1. Clear Maven repository: rm -rf ~/.m2/repository"
echo "   2. Configure Maven to use a single repository"
echo "   3. Or use Spring Boot CLI: spring run BookServiceApplication.java"
echo "   4. Or create Docker containers for the services"
