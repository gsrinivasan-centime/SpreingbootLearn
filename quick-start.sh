#!/bin/bash

# Online Bookstore Microservices Quick Start Script
# This script sets up the entire development environment

set -e  # Exit on any error

echo "🚀 Starting Online Bookstore Microservices Setup..."

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

# Check if Docker is installed and running
check_docker() {
    print_status "Checking Docker installation..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker."
        exit 1
    fi
    
    print_success "Docker is installed and running"
}

# Check if Docker Compose is installed
check_docker_compose() {
    print_status "Checking Docker Compose installation..."
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_success "Docker Compose is installed"
}

# Check if Java 17+ is installed
check_java() {
    print_status "Checking Java installation..."
    
    if ! command -v java &> /dev/null; then
        print_warning "Java is not installed. Please install Java 17 or higher."
        return 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_warning "Java version is $JAVA_VERSION. Please install Java 17 or higher."
        return 1
    fi
    
    print_success "Java $JAVA_VERSION is installed"
    return 0
}

# Check if Maven is installed
check_maven() {
    print_status "Checking Maven installation..."
    
    if ! command -v mvn &> /dev/null; then
        print_warning "Maven is not installed. Please install Maven 3.6 or higher."
        return 1
    fi
    
    print_success "Maven is installed"
    return 0
}

# Start infrastructure services
start_infrastructure() {
    print_status "Starting infrastructure services (MySQL, Redis, Kafka)..."
    
    # Create network if it doesn't exist
    docker network create bookstore-network 2>/dev/null || true
    
    # Start services using docker-compose
    docker-compose up -d mysql redis zookeeper kafka
    
    print_status "Waiting for services to be ready..."
    
    # Wait for MySQL
    print_status "Waiting for MySQL database..."
    until docker exec bookstore-mysql mysqladmin ping -h"localhost" --silent; do
        echo -n "."
        sleep 2
    done
    echo ""
    print_success "MySQL database is ready"
    
    # Wait for Redis
    print_status "Waiting for Redis..."
    until docker exec bookstore-redis redis-cli -a redis_password ping &>/dev/null; do
        echo -n "."
        sleep 2
    done
    echo ""
    print_success "Redis is ready"
    
    # Wait for Kafka
    print_status "Waiting for Kafka..."
    until docker exec bookstore-kafka kafka-topics --bootstrap-server localhost:9092 --list &> /dev/null; do
        echo -n "."
        sleep 5
    done
    echo ""
    print_success "Kafka is ready"
}

# Create Kafka topics
create_kafka_topics() {
    print_status "Creating Kafka topics..."
    
    # Topics configuration: topic_name:partitions:replication_factor
    topics=(
        "book-events:3:1"
        "order-events:3:1"
        "inventory-events:3:1"
        "user-events:3:1"
        "dead-letter-queue:1:1"
    )
    
    for topic_config in "${topics[@]}"; do
        IFS=':' read -r topic partitions replicas <<< "$topic_config"
        
        if docker exec bookstore-kafka kafka-topics --bootstrap-server localhost:9092 --list | grep -q "^$topic$"; then
            print_status "Topic '$topic' already exists"
        else
            docker exec bookstore-kafka kafka-topics \
                --bootstrap-server localhost:9092 \
                --create \
                --topic "$topic" \
                --partitions "$partitions" \
                --replication-factor "$replicas"
            print_success "Created topic: $topic"
        fi
    done
}

# Build microservices (if Java and Maven are available)
build_services() {
    if check_java && check_maven; then
        print_status "Building microservices..."
        
        # Build Book Service
        if [ -d "book-service" ]; then
            print_status "Building Book Service..."
            cd book-service
            mvn clean compile -q
            cd ..
            print_success "Book Service built successfully"
        fi
        
        # Build User Service
        if [ -d "user-service" ]; then
            print_status "Building User Service..."
            cd user-service
            mvn clean compile -q
            cd ..
            print_success "User Service built successfully"
        fi
    else
        print_warning "Skipping microservices build (Java/Maven not available)"
    fi
}

# Start monitoring services
start_monitoring() {
    print_status "Starting monitoring services..."
    
    docker-compose up -d kafka-ui redis-commander
    
    print_success "Monitoring services started"
}

# Display service URLs
show_service_urls() {
    echo ""
    print_success "🎉 Online Bookstore Microservices Setup Complete!"
    echo ""
    echo "📊 Service URLs:"
    echo "   • Kafka UI:           http://localhost:8080"
    echo "   • Redis Commander:    http://localhost:8090"
    echo ""
    echo "🔧 Database Connections:"
    echo "   • MySQL:              localhost:3308 (user: bookstore_user, password: bookstore_pass)"
    echo "   • Redis:              localhost:6379 (password: redis_password)"
    echo "   • Kafka:              localhost:9092"
    echo ""
    echo "📚 Learning Resources:"
    echo "   • Documentation:      docs/"
    echo "   • Book Service:       book-service/"
    echo "   • User Service:       user-service/"
    echo ""
    echo "🚀 To start the microservices:"
    if check_java && check_maven; then
        echo "   cd book-service && mvn spring-boot:run"
        echo "   cd user-service && mvn spring-boot:run"
    else
        echo "   Install Java 17+ and Maven, then run:"
        echo "   cd book-service && mvn spring-boot:run"
        echo "   cd user-service && mvn spring-boot:run"
    fi
    echo ""
    echo "📖 API Documentation (after starting services):"
    echo "   • Book Service Swagger:   http://localhost:8081/api/v1/swagger-ui.html"
    echo "   • Book Service API Docs:  http://localhost:8081/api/v1/api-docs"
    echo "   • User Service Swagger:   http://localhost:8082/api/v1/swagger-ui.html"
    echo "   • User Service API Docs:  http://localhost:8082/api/v1/api-docs"
    echo ""
    echo "🔑 Default Authentication:"
    echo "   • Username: admin"
    echo "   • Password: admin123"
    echo ""
}

# Stop services function
stop_services() {
    print_status "Stopping all services..."
    docker-compose down
    print_success "All services stopped"
}

# Cleanup function
cleanup() {
    print_status "Cleaning up containers and volumes..."
    docker-compose down -v
    docker system prune -f
    print_success "Cleanup completed"
}

# Main execution
main() {
    case "${1:-start}" in
        "start")
            check_docker
            check_docker_compose
            start_infrastructure
            create_kafka_topics
            build_services
            start_monitoring
            show_service_urls
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            sleep 2
            main start
            ;;
        "cleanup")
            cleanup
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
        "help")
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  start     Start all services (default)"
            echo "  stop      Stop all services"
            echo "  restart   Restart all services"
            echo "  status    Show service status"
            echo "  logs      Show logs (optionally for specific service)"
            echo "  cleanup   Stop services and remove volumes"
            echo "  help      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Start all services"
            echo "  $0 stop               # Stop all services"
            echo "  $0 logs kafka         # Show Kafka logs"
            echo "  $0 cleanup            # Clean up everything"
            ;;
        *)
            print_error "Unknown command: $1"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Execute main function with all arguments
main "$@"
