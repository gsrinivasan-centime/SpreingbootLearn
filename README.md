# Online Bookstore Microservices Application

This project demonstrates a complete microservices architecture for an online bookstore application, designed to help you prepare for Spring Boot interviews.

## 🚀 Quick Start

**New to this project? Start here:**

### ⚡ 60-Second Setup
```bash
git clone <your-repo-url>
cd SpreingbootLearn
./setup-menu.sh
```

**Or see**: [**GETTING_STARTED.md**](GETTING_STARTED.md) | [**QUICK_SETUP.md**](QUICK_SETUP.md)

---

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐
│   Book Service  │    │   User Service  │
│   (Port: 8081)  │    │   (Port: 8082)  │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │     API Gateway       │
         │     (Port: 8080)      │
         └───────────────────────┘
                     │
         ┌───────────▼───────────┐
         │    Load Balancer      │
         └───────────────────────┘
```

## Microservices

### 1. Book Service (Port: 8081)
- Manages books, categories, inventory
- MySQL Database: `bookstore_books`
- Redis Cache for book data
- Kafka Producer for book events

### 2. User Service (Port: 8082)
- Manages users, authentication, orders
- MySQL Database: `bookstore_users`
- Redis Session management
- Kafka Consumer for book events

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Database**: MySQL 8.0
- **ORM**: Hibernate/JPA
- **Caching**: Redis
- **Message Queue**: Apache Kafka
- **Database Migration**: Liquibase
- **Documentation**: Swagger/OpenAPI 3
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build Tool**: Maven
- **Containerization**: Docker, Kubernetes

## Deployment Options

### 🐳 1. Complete Docker Deployment (Recommended for Quick Start)
```bash
./docker-setup.sh
```
- **Best for**: First-time setup, demos, testing complete stack
- **Includes**: All services containerized (infrastructure + microservices)
- **Pros**: One-command setup, isolated environments, production-like
- **Cons**: Longer rebuild times during development

### 🛠️ 2. Hybrid Development Setup
```bash
./quick-start.sh                    # Start infrastructure
cd book-service && mvn spring-boot:run    # Run locally
cd user-service && mvn spring-boot:run    # Run locally
```
- **Best for**: Active development, debugging, faster code changes
- **Includes**: Docker infrastructure + local microservices
- **Pros**: Fast development cycle, easy debugging, hot reload
- **Cons**: Requires Java/Maven setup

### ☸️ 3. Kubernetes Deployment (Production Ready)
```bash
./deploy-kubernetes.sh
```
- **Best for**: Production environments, scaling, orchestration
- **Includes**: Pod-based deployment with service discovery
- **Pros**: Auto-scaling, rolling updates, health management
- **Cons**: More complex setup, requires Kubernetes cluster

## Why Microservices in Containers/Pods Work

### Container Benefits
- **Isolation**: Process, network, and filesystem separation
- **Consistency**: Same environment across dev/test/prod
- **Resource Control**: CPU and memory limits per service
- **Scalability**: Independent scaling of services

### Pod Architecture (Kubernetes)
```
Pod: bookstore-microservices
├── book-service container (port 8081)
├── user-service container (port 8082)
└── Shared: network, storage, lifecycle
```

**Key Advantages:**
- **Localhost Communication**: Services communicate via `http://localhost:8081|8082`
- **Shared Resources**: Network namespace, storage volumes
- **Unified Lifecycle**: Deploy, scale, update together
- **Resource Efficiency**: Shared infrastructure within pod

See [docs/11-Containerization.md](docs/11-Containerization.md) for detailed explanation.

## Learning Areas Covered

1. [Spring Boot Fundamentals](docs/01-SpringBoot.md)
2. [Hibernate & JPA](docs/02-Hibernate.md)
3. [Database Design & Management](docs/03-Database.md)
4. [Liquibase Migration](docs/04-Liquibase.md)
5. [Redis Caching](docs/05-Redis.md)
6. [Kafka Messaging](docs/06-Kafka.md)
7. [Swagger Documentation](docs/07-Swagger.md)
8. [Design Patterns](docs/08-DesignPatterns.md)
9. [Testing Strategies](docs/09-Testing.md)

## Quick Setup Options

### 🚀 Option 1: Complete Docker Setup (Recommended)
One-command setup that builds and runs everything:
```bash
# Clone and setup
git clone <your-repo-url>
cd SpreingbootLearn

# Run complete setup
./docker-setup.sh
```

This will:
- Build Docker images for both microservices
- Start all infrastructure (MySQL, Redis, Kafka)
- Deploy both microservices
- Create Kafka topics
- Set up database with Liquibase migrations
- Show all service URLs and status

### 🛠️ Option 2: Infrastructure + Local Development
Start infrastructure with Docker, run microservices locally:
```bash
# Start infrastructure only
./quick-start.sh

# In separate terminals:
cd book-service && mvn spring-boot:run
cd user-service && mvn spring-boot:run
```

### 📊 Option 3: Check Status
```bash
./status-check.sh        # Complete status report
./status-check.sh health # Health checks only
```

## Service URLs (After Setup)

- **📚 Book Service API**: http://localhost:8081/api/v1
- **👥 User Service API**: http://localhost:8082/api/v1
- **📖 Book Service Swagger**: http://localhost:8081/api/v1/swagger-ui/index.html
- **📖 User Service Swagger**: http://localhost:8082/api/v1/swagger-ui/index.html
- **📊 Kafka UI**: http://localhost:8080
- **🗄️ Redis Commander**: http://localhost:8090

## Interview Ready Features

- ✅ RESTful APIs with proper HTTP methods
- ✅ Database separation and connection pooling
- ✅ Redis caching at multiple levels
- ✅ Kafka event-driven communication
- ✅ Circuit breaker pattern
- ✅ Data encryption/decryption
- ✅ Idempotency handling
- ✅ Asynchronous processing
- ✅ Comprehensive testing
- ✅ API documentation
- ✅ Database migrations

## Project Structure

```
online-bookstore/
├── docs/                          # Learning documentation
├── book-service/                  # Book microservice
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
├── user-service/                  # User microservice
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
├── api-gateway/                   # API Gateway
├── docker-compose.yml             # Infrastructure services
└── README.md
```
