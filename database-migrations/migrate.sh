#!/bin/bash

# Database Migration Scripts for Bookstore Application
# This script provides easy commands to manage database migrations

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="bookstore_books"
DB_USER="bookstore_user"
DB_PASS="bookstore_pass"
MIGRATION_DIR="/Users/srinivasang/Documents/GitHub/SpreingbootLearn/database-migrations"

# Functions
print_header() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  Bookstore Database Migration Manager${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

check_prerequisites() {
    print_info "Checking prerequisites..."
    
    # Check if Docker is running
    if ! docker ps >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    # Check if database containers are running
    if ! docker ps | grep -q "bookstore-mysql"; then
        print_error "MySQL container is not running. Please start the containers first."
        print_info "Run: docker-compose up -d"
        exit 1
    fi
    
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

wait_for_database() {
    print_info "Waiting for database to be ready..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec bookstore-mysql mysql -u${DB_USER} -p${DB_PASS} -e "SELECT 1;" >/dev/null 2>&1; then
            print_success "Database is ready"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    print_error "Database is not ready after ${max_attempts} attempts"
    exit 1
}

run_migrations() {
    print_info "Running database migrations..."
    
    cd "$MIGRATION_DIR"
    
    if mvn liquibase:update -q; then
        print_success "Migrations completed successfully"
    else
        print_error "Migration failed"
        exit 1
    fi
}

show_migration_status() {
    print_info "Checking migration status..."
    
    cd "$MIGRATION_DIR"
    mvn liquibase:status
}

rollback_migrations() {
    local count=${1:-1}
    print_warning "Rolling back $count migration(s)..."
    
    cd "$MIGRATION_DIR"
    
    if mvn liquibase:rollback -Dliquibase.rollbackCount=$count; then
        print_success "Rollback completed successfully"
    else
        print_error "Rollback failed"
        exit 1
    fi
}

validate_migrations() {
    print_info "Validating migrations..."
    
    cd "$MIGRATION_DIR"
    
    if mvn liquibase:validate; then
        print_success "Migrations are valid"
    else
        print_error "Migration validation failed"
        exit 1
    fi
}

show_help() {
    print_header
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  migrate     - Run all pending migrations"
    echo "  status      - Show migration status"
    echo "  validate    - Validate migration files"
    echo "  rollback    - Rollback last migration"
    echo "  rollback N  - Rollback N migrations"
    echo "  help        - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 migrate"
    echo "  $0 status"
    echo "  $0 rollback 2"
    echo ""
}

# Main execution
case "${1:-help}" in
    "migrate")
        print_header
        check_prerequisites
        wait_for_database
        run_migrations
        ;;
    "status")
        print_header
        check_prerequisites
        wait_for_database
        show_migration_status
        ;;
    "validate")
        print_header
        check_prerequisites
        validate_migrations
        ;;
    "rollback")
        print_header
        check_prerequisites
        wait_for_database
        rollback_migrations "${2:-1}"
        ;;
    "help"|*)
        show_help
        ;;
esac
