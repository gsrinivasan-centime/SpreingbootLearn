#!/bin/bash

# Setup Options Menu for Spring Boot Microservices
# This script helps users choose the best setup option for their needs

echo "🚀 Spring Boot Microservices Setup Options"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_option() {
    echo -e "${BLUE}$1${NC} $2"
}

print_highlight() {
    echo -e "${GREEN}$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}$1${NC}"
}

echo "Choose your setup option:"
echo ""

print_option "1." "🐳 Complete Docker Setup (Recommended for beginners)"
echo "   • One-command setup with Docker containers"
echo "   • Fastest way to get everything running"
echo "   • No local Java/Maven required"
echo "   • Best for: First-time users, demos, testing"
echo ""

print_option "2." "🏭 Production-Ready Setup (Advanced)"
echo "   • Enhanced Docker setup with monitoring"
echo "   • Resource optimization and security"
echo "   • Performance tuning and health checks"
echo "   • Best for: Production evaluation, performance testing"
echo ""

print_option "3." "🛠️ Hybrid Development Setup"
echo "   • Docker infrastructure + local microservices"
echo "   • Fast development cycle with hot reload"
echo "   • Easy debugging and IDE integration"
echo "   • Best for: Active development, debugging"
echo ""

print_option "4." "📊 Status Check Only"
echo "   • Check current service status"
echo "   • View service URLs and health"
echo "   • Troubleshooting information"
echo "   • Best for: Monitoring existing setup"
echo ""

print_option "5." "🧹 Cleanup All Services"
echo "   • Stop all running services"
echo "   • Remove containers and volumes"
echo "   • Clean slate for fresh start"
echo "   • Best for: Resetting environment"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

read -p "Enter your choice (1-5): " choice

case $choice in
    1)
        print_highlight "🐳 Starting Complete Docker Setup..."
        echo ""
        echo "This will:"
        echo "• Build Docker images for microservices"
        echo "• Start all infrastructure services"
        echo "• Deploy microservices in containers"
        echo "• Create sample data"
        echo ""
        read -p "Continue? (y/N): " confirm
        if [[ $confirm =~ ^[Yy]$ ]]; then
            if [ -f "./docker-setup.sh" ]; then
                ./docker-setup.sh
            else
                print_warning "Error: docker-setup.sh not found. Please ensure you're in the project root directory."
            fi
        else
            echo "Setup cancelled."
        fi
        ;;
    2)
        print_highlight "🏭 Starting Production-Ready Setup..."
        echo ""
        echo "This will:"
        echo "• Set up production-like environment"
        echo "• Configure resource limits and monitoring"
        echo "• Apply performance optimizations"
        echo "• Enable enhanced security features"
        echo ""
        read -p "Continue? (y/N): " confirm
        if [[ $confirm =~ ^[Yy]$ ]]; then
            if [ -f "./production-setup.sh" ]; then
                ./production-setup.sh
            else
                print_warning "Error: production-setup.sh not found. Please ensure you're in the project root directory."
            fi
        else
            echo "Setup cancelled."
        fi
        ;;
    3)
        print_highlight "🛠️ Starting Hybrid Development Setup..."
        echo ""
        echo "Prerequisites:"
        echo "• Java 17+ installed"
        echo "• Maven 3.8+ installed"
        echo ""
        echo "This will:"
        echo "• Start infrastructure services with Docker"
        echo "• Provide instructions for running microservices locally"
        echo ""
        read -p "Continue? (y/N): " confirm
        if [[ $confirm =~ ^[Yy]$ ]]; then
            if [ -f "./quick-start.sh" ]; then
                ./quick-start.sh
                echo ""
                print_highlight "Infrastructure started! Now run microservices locally:"
                echo "Terminal 1: cd book-service && mvn spring-boot:run"
                echo "Terminal 2: cd user-service && mvn spring-boot:run"
            else
                print_warning "Error: quick-start.sh not found. Please ensure you're in the project root directory."
            fi
        else
            echo "Setup cancelled."
        fi
        ;;
    4)
        print_highlight "📊 Checking Service Status..."
        echo ""
        if [ -f "./status-check.sh" ]; then
            ./status-check.sh
        else
            print_warning "Error: status-check.sh not found. Trying docker-compose ps..."
            docker-compose ps 2>/dev/null || echo "No services running or docker-compose not available."
        fi
        ;;
    5)
        print_highlight "🧹 Cleaning Up All Services..."
        echo ""
        print_warning "⚠️  WARNING: This will remove all containers, volumes, and data!"
        echo ""
        read -p "Are you sure? Type 'yes' to confirm: " confirm
        if [[ $confirm == "yes" ]]; then
            echo "Stopping and removing all services..."
            docker-compose down -v 2>/dev/null || true
            docker system prune -f 2>/dev/null || true
            print_highlight "✅ Cleanup completed!"
        else
            echo "Cleanup cancelled."
        fi
        ;;
    *)
        print_warning "Invalid choice. Please run the script again and select 1-5."
        exit 1
        ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
print_highlight "📖 Quick Reference:"
echo ""
echo "Service URLs (after setup):"
echo "• Book Service API: http://localhost:8081/api/v1"
echo "• User Service API: http://localhost:8082/api/v1"
echo "• Kafka UI: http://localhost:8080"
echo "• Redis Commander: http://localhost:8090"
echo ""
echo "Management commands:"
echo "• Status check: ./status-check.sh"
echo "• View logs: docker-compose logs -f"
echo "• Stop services: docker-compose down"
echo ""
echo "Documentation:"
echo "• Complete setup guide: QUICK_SETUP.md"
echo "• Architecture docs: docs/"
echo "• Project README: README.md"
echo ""
print_highlight "🎉 Happy coding!"
