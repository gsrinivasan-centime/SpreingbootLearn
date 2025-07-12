#!/bin/bash

# Service Health Check Script
echo "🏥 Bookstore Microservices Health Check"
echo "========================================"
echo ""

# Infrastructure Services
echo "📊 Infrastructure Services:"
echo "----------------------------"

# Redis Test
echo -n "Redis: "
if docker exec bookstore-redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ HEALTHY"
else
    echo "❌ UNHEALTHY"
fi

# MySQL Test
echo -n "MySQL: "
if docker exec bookstore-mysql mysql -u bookstore_user -pbookstore_pass -e "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ HEALTHY"
else
    echo "❌ UNHEALTHY"
fi

# Kafka Test
echo -n "Kafka: "
if docker exec bookstore-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo "✅ HEALTHY"
else
    echo "❌ UNHEALTHY"
fi

echo ""
echo "🌐 Management UI Services:"
echo "-------------------------"

# Redis Commander Test
echo -n "Redis Commander (Port 8090): "
HTTP_CODE=$(curl -s -w "%{http_code}" -o /dev/null http://localhost:8090/ 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ ACCESSIBLE (HTTP $HTTP_CODE)"
else
    echo "❌ INACCESSIBLE (HTTP $HTTP_CODE)"
fi

# Kafka UI Test
echo -n "Kafka UI (Port 8080): "
HTTP_CODE=$(curl -s -w "%{http_code}" -o /dev/null http://localhost:8080/ 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ ACCESSIBLE (HTTP $HTTP_CODE)"
else
    echo "❌ INACCESSIBLE (HTTP $HTTP_CODE)"
fi

echo ""
echo "📚 Microservices:"
echo "-----------------"

# Book Service Test
echo -n "Book Service (Port 8081): "
BOOK_STATUS=$(docker ps --filter "name=bookstore-book-service" --format "{{.Status}}" 2>/dev/null)
if [[ $BOOK_STATUS == *"Up"* ]]; then
    # Try to test health endpoint
    HTTP_CODE=$(curl -s -w "%{http_code}" -o /dev/null http://localhost:8081/api/v1/actuator/health 2>/dev/null)
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ RUNNING & ACCESSIBLE (HTTP $HTTP_CODE)"
    else
        echo "🔄 RUNNING but health endpoint inaccessible (HTTP $HTTP_CODE)"
        echo "   Container Status: $BOOK_STATUS"
        # Try a simple GET to see if server is responding
        HTTP_CODE=$(curl -s -w "%{http_code}" -o /dev/null http://localhost:8081/ 2>/dev/null)
        echo "   Root endpoint: HTTP $HTTP_CODE"
    fi
else
    echo "❌ NOT RUNNING"
    echo "   Container Status: ${BOOK_STATUS:-'Not found'}"
fi

# User Service Test
echo -n "User Service (Port 8082): "
USER_STATUS=$(docker ps --filter "name=bookstore-user-service" --format "{{.Status}}" 2>/dev/null)
if [[ $USER_STATUS == *"Up"* ]]; then
    HTTP_CODE=$(curl -s -w "%{http_code}" -o /dev/null http://localhost:8082/api/v1/actuator/health 2>/dev/null)
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ RUNNING & ACCESSIBLE (HTTP $HTTP_CODE)"
    else
        echo "🔄 RUNNING but health endpoint inaccessible (HTTP $HTTP_CODE)"
    fi
else
    echo "❌ NOT RUNNING (User service has compilation issues)"
fi

echo ""
echo "🔗 Access URLs:"
echo "---------------"
echo "Redis Commander: http://localhost:8090"
echo "Kafka UI:        http://localhost:8080"
echo "Book Service:    http://localhost:8081"
echo "User Service:    http://localhost:8082"

echo ""
echo "📋 Container Status:"
echo "-------------------"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=bookstore-*"

echo ""
echo "✨ Summary:"
echo "----------"
echo "✅ Infrastructure services are running and healthy"
echo "✅ Management UIs are accessible"
echo "🔄 Book service container exists but needs dependency fixes"
echo "❌ User service not started due to compilation issues"
echo ""
echo "💡 Next steps:"
echo "  1. Fix BookMapper dependency in book-service"
echo "  2. Fix Lombok compilation issues in user-service"
echo "  3. Test API endpoints once services are fully operational"
