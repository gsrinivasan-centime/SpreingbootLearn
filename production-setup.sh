#!/bin/bash

# Production-Ready Docker Setup Script
# This script sets up the microservices with production-like configurations

set -e  # Exit on any error

echo "🏭 Starting Production-Ready Microservices Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check system requirements
check_system_requirements() {
    print_status "Checking system requirements..."
    
    # Check available memory
    local available_memory=$(free -m | awk 'NR==2{printf "%.1f", $7/1024}')
    if (( $(echo "$available_memory < 4.0" | bc -l) )); then
        print_warning "Available memory is ${available_memory}GB. Recommended: 4GB+"
    else
        print_success "Available memory: ${available_memory}GB"
    fi
    
    # Check Docker version
    local docker_version=$(docker --version | grep -oE '[0-9]+\.[0-9]+' | head -1)
    print_success "Docker version: $docker_version"
    
    # Check available disk space
    local available_space=$(df -h . | awk 'NR==2 {print $4}')
    print_success "Available disk space: $available_space"
}

# Setup environment with enhanced configuration
setup_production_environment() {
    print_status "Setting up production environment..."
    
    # Create necessary directories
    mkdir -p logs
    mkdir -p monitoring
    mkdir -p backups
    
    # Copy enhanced Docker Compose configuration
    if [ -f "docker-compose-enhanced.yml" ]; then
        cp docker-compose-enhanced.yml docker-compose.override.yml
        print_success "Enhanced configuration activated"
    else
        print_warning "Enhanced configuration not found, using default"
    fi
    
    # Set environment variables for production
    export COMPOSE_PROJECT_NAME=bookstore-prod
    export SPRING_PROFILES_ACTIVE=docker,prod
    
    print_success "Production environment configured"
}

# Enhanced service startup with monitoring
start_services_with_monitoring() {
    print_status "Starting services with enhanced monitoring..."
    
    # Pull latest images
    print_status "Pulling latest Docker images..."
    docker-compose pull
    
    # Build services with optimizations
    print_status "Building microservices with production optimizations..."
    docker-compose build --parallel --compress
    
    # Start infrastructure first
    print_status "Starting infrastructure services..."
    docker-compose up -d mysql redis zookeeper kafka
    
    # Wait for infrastructure to be healthy
    print_status "Waiting for infrastructure to be ready..."
    local max_wait=300  # 5 minutes
    local waited=0
    
    while [ $waited -lt $max_wait ]; do
        local healthy_infra=$(docker-compose ps --format json | jq -r 'select(.Name | contains("mysql") or contains("redis") or contains("kafka")) | .State' | grep -c "healthy" || echo "0")
        
        if [ "$healthy_infra" -ge 3 ]; then
            print_success "Infrastructure is ready"
            break
        fi
        
        echo -n "."
        sleep 5
        ((waited += 5))
    done
    
    if [ $waited -ge $max_wait ]; then
        print_error "Infrastructure failed to start within timeout"
        exit 1
    fi
    
    # Start microservices
    print_status "Starting microservices..."
    docker-compose up -d book-service user-service
    
    # Start monitoring tools
    print_status "Starting monitoring tools..."
    docker-compose up -d kafka-ui redis-commander
    
    print_success "All services started"
}

# Setup monitoring and logging
setup_monitoring() {
    print_status "Setting up monitoring and logging..."
    
    # Create log directories for each service
    mkdir -p logs/book-service
    mkdir -p logs/user-service
    mkdir -p logs/mysql
    mkdir -p logs/kafka
    
    # Setup log rotation (basic)
    cat > logs/logrotate.conf << 'EOF'
/path/to/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    notifempty
    create 644 root root
    postrotate
        /usr/bin/docker-compose restart book-service user-service
    endscript
}
EOF
    
    print_success "Monitoring and logging configured"
}

# Performance tuning
apply_performance_tuning() {
    print_status "Applying performance tuning..."
    
    # MySQL performance tuning
    print_status "Tuning MySQL performance..."
    docker exec bookstore-mysql mysql -u root -proot_password -e "
        SET GLOBAL innodb_buffer_pool_size = 268435456;  -- 256MB
        SET GLOBAL max_connections = 200;
        SET GLOBAL query_cache_size = 67108864;          -- 64MB
    " 2>/dev/null || true
    
    # Redis optimization
    print_status "Optimizing Redis configuration..."
    docker exec bookstore-redis redis-cli -a redis_password CONFIG SET maxmemory-policy allkeys-lru 2>/dev/null || true
    docker exec bookstore-redis redis-cli -a redis_password CONFIG SET save "900 1 300 10 60 10000" 2>/dev/null || true
    
    print_success "Performance tuning applied"
}

# Health check and validation
comprehensive_health_check() {
    print_status "Running comprehensive health checks..."
    
    # Wait for services to fully start
    sleep 30
    
    local all_healthy=true
    
    # Check each service
    services=("mysql" "redis" "kafka" "book-service" "user-service")
    
    for service in "${services[@]}"; do
        local health_status=""
        case $service in
            "mysql")
                health_status=$(docker exec bookstore-mysql mysqladmin ping -h"localhost" --silent && echo "healthy" || echo "unhealthy")
                ;;
            "redis")
                health_status=$(docker exec bookstore-redis redis-cli -a redis_password ping 2>/dev/null | grep -q "PONG" && echo "healthy" || echo "unhealthy")
                ;;
            "kafka")
                health_status=$(docker exec bookstore-kafka kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null && echo "healthy" || echo "unhealthy")
                ;;
            "book-service")
                health_status=$(curl -s -f http://localhost:8081/api/v1/actuator/health/readiness >/dev/null && echo "healthy" || echo "unhealthy")
                ;;
            "user-service")
                health_status=$(curl -s -f http://localhost:8082/api/v1/actuator/health/readiness >/dev/null && echo "healthy" || echo "unhealthy")
                ;;
        esac
        
        if [ "$health_status" = "healthy" ]; then
            print_success "$service is healthy"
        else
            print_error "$service is unhealthy"
            all_healthy=false
        fi
    done
    
    if [ "$all_healthy" = true ]; then
        print_success "All services are healthy"
        return 0
    else
        print_error "Some services are unhealthy"
        return 1
    fi
}

# Create sample data
create_sample_data() {
    print_status "Creating sample data..."
    
    # Wait a bit more for services to be fully ready
    sleep 10
    
    # Test Book Service API
    local book_response=$(curl -s -X POST http://localhost:8081/api/v1/books \
        -H "Content-Type: application/json" \
        -d '{
            "title": "Spring Boot in Action",
            "author": "Craig Walls",
            "isbn": "978-1617292545",
            "price": 39.99,
            "category": "Technology"
        }' 2>/dev/null || echo "")
    
    if [ -n "$book_response" ]; then
        print_success "Sample book created"
    else
        print_warning "Could not create sample book (service may still be starting)"
    fi
    
    # Test User Service API
    local user_response=$(curl -s -X POST http://localhost:8082/api/v1/users \
        -H "Content-Type: application/json" \
        -d '{
            "username": "testuser",
            "email": "test@example.com",
            "password": "testpass123"
        }' 2>/dev/null || echo "")
    
    if [ -n "$user_response" ]; then
        print_success "Sample user created"
    else
        print_warning "Could not create sample user (service may still be starting)"
    fi
}

# Display production dashboard
show_production_dashboard() {
    echo ""
    echo "🏭 Production-Ready Microservices Dashboard"
    echo "============================================"
    echo ""
    
    # Service status
    print_status "Service Status:"
    docker-compose ps --format "table {{.Name}}\\t{{.Status}}\\t{{.Ports}}"
    
    echo ""
    echo "🌐 Application URLs:"
    echo "  📚 Book Service API:      http://localhost:8081/api/v1"
    echo "  👥 User Service API:      http://localhost:8082/api/v1"
    echo "  📊 Kafka UI:              http://localhost:8080 (admin/admin123)"
    echo "  🗄️  Redis Commander:      http://localhost:8090 (admin/admin123)"
    echo ""
    echo "📖 API Documentation:"
    echo "  📚 Book Service Swagger:  http://localhost:8081/api/v1/swagger-ui/index.html"
    echo "  👥 User Service Swagger:  http://localhost:8082/api/v1/swagger-ui/index.html"
    echo ""
    echo "🔍 Health Endpoints:"
    echo "  📚 Book Service Health:   http://localhost:8081/api/v1/actuator/health"
    echo "  👥 User Service Health:   http://localhost:8082/api/v1/actuator/health"
    echo "  📊 Book Service Metrics:  http://localhost:8081/api/v1/actuator/metrics"
    echo "  📈 User Service Metrics:  http://localhost:8082/api/v1/actuator/metrics"
    echo ""
    echo "🛠️  Management Commands:"
    echo "  • View logs:              docker-compose logs -f [service]"
    echo "  • Restart service:        docker-compose restart [service]"
    echo "  • Scale service:          docker-compose up -d --scale book-service=2"
    echo "  • Stop all:               docker-compose down"
    echo "  • Full reset:             docker-compose down -v"
    echo ""
    echo "🔧 Resource Monitoring:"
    echo "  • Container stats:        docker stats"
    echo "  • Service health:         ./status-check.sh health"
    echo "  • Full status:            ./status-check.sh"
    echo ""
    print_success "Production environment is ready! 🚀"
}

# Cleanup function
cleanup_on_error() {
    if [ $? -ne 0 ]; then
        print_error "Setup failed. Checking logs..."
        docker-compose logs --tail=50
        print_error "Use 'docker-compose down -v' to clean up"
    fi
}

trap cleanup_on_error EXIT

# Main execution
main() {
    case "${1:-deploy}" in
        "deploy" | "start")
            check_system_requirements
            setup_production_environment
            start_services_with_monitoring
            setup_monitoring
            apply_performance_tuning
            if comprehensive_health_check; then
                create_sample_data
                show_production_dashboard
            else
                print_error "Health checks failed. Check logs with: docker-compose logs"
                exit 1
            fi
            ;;
        "stop")
            print_status "Stopping production environment..."
            docker-compose down
            print_success "Production environment stopped"
            ;;
        "restart")
            print_status "Restarting production environment..."
            docker-compose restart
            sleep 15
            comprehensive_health_check && show_production_dashboard
            ;;
        "health")
            comprehensive_health_check
            ;;
        "logs")
            if [ -n "$2" ]; then
                docker-compose logs -f "$2"
            else
                docker-compose logs -f
            fi
            ;;
        "monitor")
            watch -n 5 'docker stats --no-stream'
            ;;
        "backup")
            print_status "Creating backup..."
            mkdir -p backups/$(date +%Y%m%d_%H%M%S)
            docker exec bookstore-mysql mysqldump -u root -proot_password --all-databases > backups/$(date +%Y%m%d_%H%M%S)/mysql_backup.sql
            print_success "Backup created"
            ;;
        "cleanup")
            print_status "Full cleanup..."
            docker-compose down -v
            docker system prune -af
            rm -rf logs/*
            print_success "Cleanup completed"
            ;;
        "help")
            echo "Production-Ready Microservices Setup"
            echo ""
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  deploy    Deploy production environment (default)"
            echo "  start     Same as deploy"
            echo "  stop      Stop all services"
            echo "  restart   Restart all services"
            echo "  health    Run health checks"
            echo "  logs      Show logs (optionally for specific service)"
            echo "  monitor   Monitor resource usage"
            echo "  backup    Create database backup"
            echo "  cleanup   Full cleanup (removes all data)"
            echo "  help      Show this help message"
            ;;
        *)
            print_error "Unknown command: $1"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"
