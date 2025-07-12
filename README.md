# Online Bookstore Microservices Application

This project demonstrates a complete microservices architecture for an online bookstore application, designed to help you prepare for Spring Boot interviews.

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

### 1. Traditional JVM Deployment
```bash
./start-microservices.sh
```
- Runs services directly as JAR files
- Requires local MySQL, Redis, Kafka setup
- Good for development and testing

### 2. Docker Compose (Recommended for Development)
```bash
./start-containers.sh
```
- Containerized microservices with infrastructure
- Isolated environments
- Easy setup and teardown
- Includes management tools (Redis Commander, Kafka UI)

### 3. Kubernetes (Production Ready)
```bash
./deploy-kubernetes.sh
```
- Pod-based deployment with both services in same pod
- Service discovery and load balancing
- Auto-scaling and rolling updates
- Production-grade orchestration

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

## Quick Start

1. Start infrastructure services:
   ```bash
   docker-compose up -d mysql redis kafka zookeeper
   ```

2. Run Book Service:
   ```bash
   cd book-service
   mvn spring-boot:run
   ```

3. Run User Service:
   ```bash
   cd user-service
   mvn spring-boot:run
   ```

4. Access Swagger UI:
   - Book Service: http://localhost:8081/swagger-ui.html
   - User Service: http://localhost:8082/swagger-ui.html

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
