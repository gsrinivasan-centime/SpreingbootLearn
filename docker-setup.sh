#!/bin/bash

# Docker-based Complete Setup Script
# This script sets up the entire application stack using Docker only

set -e  # Exit on any error

echo "🐳 Starting Complete Docker-based Microservices Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Check Docker prerequisites
check_docker() {
    print_status "Checking Docker installation..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker Desktop first."
        echo "Download from: https://www.docker.com/products/docker-desktop"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker Desktop."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose."
        exit 1
    fi
    
    print_success "Docker and Docker Compose are ready"
}

# Clean up any existing containers
cleanup_existing() {
    print_status "Cleaning up any existing containers..."
    
    # Stop and remove existing containers
    docker-compose down -v 2>/dev/null || true
    
    # Remove any orphaned containers
    docker container prune -f 2>/dev/null || true
    
    print_success "Cleanup completed"
}

# Build Docker images for microservices
build_images() {
    print_status "Building Docker images for microservices..."
    
    # Build Book Service
    if [ -d "book-service" ]; then
        print_status "Building Book Service Docker image..."
        docker-compose build book-service
        print_success "Book Service image built"
    else
        print_error "Book Service directory not found"
        exit 1
    fi
    
    # Build User Service
    if [ -d "user-service" ]; then
        print_status "Building User Service Docker image..."
        docker-compose build user-service
        print_success "User Service image built"
    else
        print_error "User Service directory not found"
        exit 1
    fi
}

# Start all services
start_services() {
    print_status "Starting all services with Docker Compose..."
    
    # Start all services
    docker-compose up -d
    
    print_status "Waiting for services to be healthy..."
    
    # Wait for all services to be healthy
    local max_attempts=60
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        local healthy_count=$(docker-compose ps --format json | jq -r '.State' | grep -c "healthy" || echo "0")
        local total_services=7  # mysql, redis, kafka, zookeeper, book-service, user-service, kafka-ui, redis-commander
        
        if [ "$healthy_count" -ge 5 ]; then  # At least core services should be healthy
            break
        fi
        
        echo -n "."
        sleep 5
        ((attempt++))
    done
    
    echo ""
    print_success "Services are starting up"
}

# Verify database setup
verify_database() {
    print_status "Verifying database setup..."
    
    # Wait a bit more for database initialization
    sleep 10
    
    # Check if databases were created
    local books_db=$(docker exec bookstore-mysql mysql -u bookstore_user -pbookstore_pass -e "SHOW DATABASES LIKE 'bookstore_books';" --skip-column-names 2>/dev/null || echo "")
    local users_db=$(docker exec bookstore-mysql mysql -u bookstore_user -pbookstore_pass -e "SHOW DATABASES LIKE 'bookstore_users';" --skip-column-names 2>/dev/null || echo "")
    
    if [ -n "$books_db" ]; then
        print_success "Books database is ready"
    else
        print_warning "Books database may still be initializing"
    fi
    
    if [ -n "$users_db" ]; then
        print_success "Users database is ready"
    else
        print_warning "Users database may still be initializing"
    fi
}

# Create Kafka topics
setup_kafka_topics() {
    print_status "Setting up Kafka topics..."
    
    # Wait for Kafka to be ready
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if docker exec bookstore-kafka kafka-topics --bootstrap-server localhost:9092 --list &> /dev/null; then
            break
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo ""
    
    # Create topics
    local topics=(
        "book-events"
        "order-events"
        "inventory-events"
        "user-events"
        "notification-events"
    )
    
    for topic in "${topics[@]}"; do
        if docker exec bookstore-kafka kafka-topics --bootstrap-server localhost:9092 --list | grep -q "^$topic$"; then
            print_status "Topic '$topic' already exists"
        else
            docker exec bookstore-kafka kafka-topics \
                --bootstrap-server localhost:9092 \
                --create \
                --topic "$topic" \
                --partitions 3 \
                --replication-factor 1 &> /dev/null
            print_success "Created topic: $topic"
        fi
    done
}

# Test API endpoints
test_endpoints() {
    print_status "Testing API endpoints..."
    
    # Wait a bit more for microservices to start
    sleep 15
    
    # Test Book Service
    local book_health=""
    local user_health=""
    
    # Try multiple times as services might still be starting
    for i in {1..10}; do
        book_health=$(curl -s http://localhost:8081/api/v1/actuator/health 2>/dev/null || echo "")
        if [ -n "$book_health" ]; then
            break
        fi
        sleep 3
    done
    
    for i in {1..10}; do
        user_health=$(curl -s http://localhost:8082/api/v1/actuator/health 2>/dev/null || echo "")
        if [ -n "$user_health" ]; then
            break
        fi
        sleep 3
    done
    
    if [ -n "$book_health" ]; then
        print_success "Book Service API is responding"
    else
        print_warning "Book Service API may still be starting"
    fi
    
    if [ -n "$user_health" ]; then
        print_success "User Service API is responding"
    else
        print_warning "User Service API may still be starting"
    fi
}

# Display final status and URLs
show_status() {
    echo ""
    echo "🎉 Docker-based Microservices Setup Complete!"
    echo ""
    
    # Show container status
    print_status "Container Status:"
    docker-compose ps
    
    echo ""
    echo "🌐 Service URLs:"
    echo "   📚 Book Service API:      http://localhost:8081/api/v1"
    echo "   👥 User Service API:      http://localhost:8082/api/v1"
    echo "   📊 Kafka UI:              http://localhost:8080"
    echo "   🗄️  Redis Commander:      http://localhost:8090"
    echo ""
    echo "📖 API Documentation:"
    echo "   📚 Book Service Swagger:  http://localhost:8081/api/v1/swagger-ui/index.html"
    echo "   👥 User Service Swagger:  http://localhost:8082/api/v1/swagger-ui/index.html"
    echo ""
    echo "🔧 Database Access:"
    echo "   🐘 MySQL:                 localhost:3308"
    echo "   🟥 Redis:                 localhost:6379"
    echo "   📨 Kafka:                 localhost:9092"
    echo ""
    echo "🔍 Health Checks:"
    echo "   📚 Book Service:          http://localhost:8081/api/v1/actuator/health"
    echo "   👥 User Service:          http://localhost:8082/api/v1/actuator/health"
    echo ""
    echo "🛠️  Management Commands:"
    echo "   • View logs:              docker-compose logs -f [service-name]"
    echo "   • Stop services:          docker-compose down"
    echo "   • Restart service:        docker-compose restart [service-name]"
    echo "   • Full cleanup:           docker-compose down -v"
    echo ""
    print_success "All services are running! 🚀"
}

# Handle cleanup on script exit
cleanup_on_exit() {
    if [ $? -ne 0 ]; then
        print_error "Setup failed. Cleaning up..."
        docker-compose down 2>/dev/null || true
    fi
}

trap cleanup_on_exit EXIT

# Main execution flow
main() {
    case "${1:-deploy}" in
        "deploy" | "start")
            check_docker
            cleanup_existing
            build_images
            start_services
            verify_database
            setup_kafka_topics
            test_endpoints
            show_status
            ;;
        "stop")
            print_status "Stopping all services..."
            docker-compose down
            print_success "All services stopped"
            ;;
        "restart")
            print_status "Restarting all services..."
            docker-compose restart
            sleep 10
            show_status
            ;;
        "cleanup")
            print_status "Full cleanup..."
            docker-compose down -v
            docker image prune -f
            print_success "Cleanup completed"
            ;;
        "status")
            docker-compose ps
            ;;
        "logs")
            if [ -n "$2" ]; then
                docker-compose logs -f "$2"
            else
                docker-compose logs -f
            fi
            ;;
        "rebuild")
            print_status "Rebuilding and redeploying..."
            docker-compose down
            docker-compose build --no-cache
            docker-compose up -d
            sleep 15
            show_status
            ;;
        "help")
            echo "Docker-based Microservices Setup Script"
            echo ""
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  deploy    Deploy complete stack (default)"
            echo "  start     Same as deploy"
            echo "  stop      Stop all services"
            echo "  restart   Restart all services"
            echo "  rebuild   Rebuild images and redeploy"
            echo "  status    Show service status"
            echo "  logs      Show logs (optionally for specific service)"
            echo "  cleanup   Stop services and remove volumes"
            echo "  help      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Deploy complete stack"
            echo "  $0 stop               # Stop all services"
            echo "  $0 logs book-service  # Show Book Service logs"
            echo "  $0 rebuild            # Rebuild and redeploy"
            ;;
        *)
            print_error "Unknown command: $1"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Run main function with arguments
main "$@"
