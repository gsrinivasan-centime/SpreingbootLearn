# Visual Documentation for Online Bookstore

This document contains flowcharts, UML diagrams, and mind maps for the Online Bookstore application.

## Table of Contents
1. [System Architecture Diagram](#system-architecture-diagram)
2. [Database ER Diagrams](#database-er-diagrams)
3. [API Flow Diagrams](#api-flow-diagrams)
4. [Microservices Communication Flow](#microservices-communication-flow)
5. [Kafka Event Flow](#kafka-event-flow)
6. [Redis Caching Strategy](#redis-caching-strategy)
7. [Mind Maps](#mind-maps)

## System Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        A[Web Browser/Mobile App]
        B[API Gateway]
    end
    
    subgraph "Microservices Layer"
        C[Book Service :8081]
        D[User Service :8082]
    end
    
    subgraph "Data Layer"
        E[(Books Database)]
        F[(Users Database)]
    end
    
    subgraph "Infrastructure Layer"
        G[Redis Cache]
        H[Kafka Message Broker]
        I[Zookeeper]
        J[Prometheus]
        K[Grafana]
        L[SonarQube]
    end
    
    A --> B
    B --> C
    B --> D
    C --> E
    D --> F
    C --> G
    D --> G
    C --> H
    D --> H
    H --> I
    C --> J
    D --> J
    J --> K
```

## Database ER Diagrams

### Books Database Schema
```mermaid
erDiagram
    BOOKS {
        bigint id PK
        varchar(255) title
        varchar(255) author
        varchar(20) isbn UK
        decimal(10,2) price
        int stock_quantity
        text description
        varchar(100) category
        datetime created_at
        datetime updated_at
        boolean active
    }
```

### Users Database Schema
```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar(100) first_name
        varchar(100) last_name
        varchar(255) email UK
        varchar(500) phone_number
        boolean active
        datetime created_at
        datetime updated_at
    }
```

## API Flow Diagrams

### Book Creation Flow
```mermaid
sequenceDiagram
    participant Client
    participant BookController
    participant BookService
    participant IdempotencyService
    participant BookRepository
    participant KafkaProducer
    participant Redis
    
    Client->>BookController: POST /api/v1/books
    BookController->>BookService: createBook(request)
    BookService->>IdempotencyService: isProcessed(key)
    IdempotencyService->>Redis: check idempotency
    Redis-->>IdempotencyService: false
    IdempotencyService-->>BookService: not processed
    BookService->>BookRepository: save(book)
    BookRepository-->>BookService: saved book
    BookService->>KafkaProducer: publishBookCreatedEvent
    BookService->>IdempotencyService: markAsProcessed(key)
    BookService->>Redis: cache book
    BookService-->>BookController: BookDto
    BookController-->>Client: 201 Created
```

### User Registration Flow
```mermaid
sequenceDiagram
    participant Client
    participant UserController
    participant UserService
    participant UserRepository
    participant EncryptionService
    participant KafkaProducer
    participant BatchProcessor
    
    Client->>UserController: POST /api/v1/users
    UserController->>UserService: createUser(request)
    UserService->>UserRepository: existsByEmail(email)
    UserRepository-->>UserService: false
    UserService->>EncryptionService: encrypt(phoneNumber)
    EncryptionService-->>UserService: encrypted phone
    UserService->>UserRepository: save(user)
    UserRepository-->>UserService: saved user
    UserService->>KafkaProducer: publishUserCreatedEvent
    UserService->>BatchProcessor: processRegistrationNotification(async)
    UserService-->>UserController: UserDto
    UserController-->>Client: 201 Created
```

## Microservices Communication Flow

### Service-to-Service Communication
```mermaid
graph TD
    A[Book Service] -->|HTTP/REST| B[User Service]
    B -->|HTTP/REST| A
    A -->|Kafka Events| C[Message Broker]
    B -->|Kafka Events| C
    C -->|CDC Events| D[External Systems]
    A -->|Cache Operations| E[Redis]
    B -->|Cache Operations| E
    E -->|Session Management| B
    E -->|Distributed Locking| A
    E -->|Distributed Locking| B
```

### Circuit Breaker Pattern Flow
```mermaid
stateDiagram-v2
    [*] --> Closed
    Closed --> Open : Failure threshold reached
    Open --> HalfOpen : Timeout period elapsed
    HalfOpen --> Closed : Success
    HalfOpen --> Open : Failure
    
    state Closed {
        [*] --> Normal
        Normal --> FailureCount : Request fails
        FailureCount --> Normal : Request succeeds
        FailureCount --> [*] : Threshold reached
    }
    
    state Open {
        [*] --> BlockingRequests
        BlockingRequests --> [*] : Timeout
    }
    
    state HalfOpen {
        [*] --> TestRequest
        TestRequest --> [*] : Result
    }
```

## Kafka Event Flow

### Event-Driven Architecture
```mermaid
graph LR
    subgraph "Book Service"
        A[Book Operations] --> B[Book Events]
    end
    
    subgraph "User Service"
        C[User Operations] --> D[User Events]
    end
    
    subgraph "Kafka Topics"
        E[book-events]
        F[user-events]
        G[book-cdc-events]
        H[user-cdc-events]
    end
    
    subgraph "Consumers"
        I[Analytics Service]
        J[Notification Service]
        K[Search Index Service]
        L[Audit Service]
    end
    
    B --> E
    D --> F
    B --> G
    D --> H
    E --> I
    F --> J
    E --> K
    F --> K
    G --> L
    H --> L
```

### CDC (Change Data Capture) Flow
```mermaid
sequenceDiagram
    participant App as Application
    participant DB as Database
    participant CDC as CDC Connector
    participant Kafka as Kafka Topic
    participant Consumer as Event Consumer
    
    App->>DB: INSERT/UPDATE/DELETE
    DB->>CDC: Database Change Log
    CDC->>Kafka: Publish CDC Event
    Kafka->>Consumer: Consume Change Event
    Consumer->>Consumer: Process Business Logic
```

## Redis Caching Strategy

### Multi-Level Caching
```mermaid
graph TD
    A[Application Request] --> B{Cache L1 Check}
    B -->|Hit| C[Return Cached Data]
    B -->|Miss| D{Cache L2 Check}
    D -->|Hit| E[Update L1 & Return]
    D -->|Miss| F[Database Query]
    F --> G[Update L1 & L2]
    G --> H[Return Data]
    
    subgraph "Cache Levels"
        I[L1: Application Cache]
        J[L2: Redis Cache]
    end
    
    subgraph "Cache Types"
        K[Entity Cache]
        L[Query Cache]
        M[Session Cache]
        N[Distributed Lock]
    end
```

### Cache Invalidation Strategy
```mermaid
graph TD
    A[Data Update] --> B[Invalidate Related Caches]
    B --> C[Clear Entity Cache]
    B --> D[Clear Query Cache]
    B --> E[Update Search Index Cache]
    C --> F[Trigger Cache Refresh]
    D --> F
    E --> F
    F --> G[Background Cache Warming]
```

## Mind Maps

### Spring Boot Learning Mind Map
```
Spring Boot Interview Prep
├── Core Concepts
│   ├── Dependency Injection
│   ├── Auto-Configuration
│   ├── Starter Dependencies
│   └── Application Properties
├── Data Access
│   ├── Spring Data JPA
│   ├── Hibernate ORM
│   ├── Database Migrations (Liquibase)
│   └── Connection Pooling
├── Caching
│   ├── Redis Integration
│   ├── Cache Abstraction
│   ├── Cache Strategies
│   └── Distributed Caching
├── Messaging
│   ├── Kafka Producer/Consumer
│   ├── Event-Driven Architecture
│   ├── Message Serialization
│   └── Error Handling
├── Security
│   ├── Data Encryption
│   ├── Input Validation
│   ├── Authentication/Authorization
│   └── Security Headers
├── Testing
│   ├── Unit Testing (JUnit)
│   ├── Integration Testing
│   ├── Contract Testing
│   └── Test Containers
├── Monitoring
│   ├── Actuator Endpoints
│   ├── Metrics (Prometheus)
│   ├── Health Checks
│   └── Distributed Tracing
└── Design Patterns
    ├── Repository Pattern
    ├── Service Layer Pattern
    ├── Circuit Breaker
    └── Idempotency Pattern
```

### Microservices Architecture Mind Map
```
Microservices Architecture
├── Service Design
│   ├── Single Responsibility
│   ├── Domain-Driven Design
│   ├── API First Approach
│   └── Database per Service
├── Communication
│   ├── Synchronous (REST/HTTP)
│   ├── Asynchronous (Messaging)
│   ├── Service Discovery
│   └── Load Balancing
├── Data Management
│   ├── Event Sourcing
│   ├── CQRS Pattern
│   ├── Saga Pattern
│   └── Distributed Transactions
├── Resilience
│   ├── Circuit Breaker
│   ├── Retry Mechanism
│   ├── Timeout Handling
│   └── Bulkhead Pattern
├── Observability
│   ├── Logging
│   ├── Metrics
│   ├── Distributed Tracing
│   └── Health Monitoring
└── Deployment
    ├── Containerization (Docker)
    ├── Orchestration (Kubernetes)
    ├── CI/CD Pipelines
    └── Blue-Green Deployment
```

### Database Design Mind Map
```
Database Design & Optimization
├── Schema Design
│   ├── Normalization
│   ├── Denormalization
│   ├── Indexing Strategy
│   └── Partitioning
├── Performance
│   ├── Query Optimization
│   ├── Connection Pooling
│   ├── Read Replicas
│   └── Caching Layers
├── Migrations
│   ├── Version Control
│   ├── Rollback Strategies
│   ├── Zero-Downtime Deployments
│   └── Data Consistency
├── Monitoring
│   ├── Query Performance
│   ├── Connection Metrics
│   ├── Resource Utilization
│   └── Error Tracking
└── Security
    ├── Access Controls
    ├── Data Encryption
    ├── Audit Logging
    └── Backup Strategies
```

## Technology Integration Diagram

```mermaid
graph TB
    subgraph "Development Stack"
        A[Java 17] --> B[Spring Boot 3.x]
        B --> C[Spring Data JPA]
        B --> D[Spring Security]
        B --> E[Spring Cache]
        B --> F[Spring Kafka]
    end
    
    subgraph "Database Stack"
        G[MySQL 8.0] --> H[Liquibase]
        G --> I[HikariCP]
        G --> J[Hibernate]
    end
    
    subgraph "Caching Stack"
        K[Redis 7.x] --> L[Redisson]
        K --> M[Spring Session]
        K --> N[Jedis/Lettuce]
    end
    
    subgraph "Messaging Stack"
        O[Apache Kafka] --> P[Zookeeper]
        O --> Q[Schema Registry]
        O --> R[Kafka Connect]
    end
    
    subgraph "Monitoring Stack"
        S[Prometheus] --> T[Grafana]
        S --> U[Alert Manager]
        V[ELK Stack] --> W[Kibana]
    end
    
    subgraph "Quality Stack"
        X[SonarQube] --> Y[Code Quality]
        Z[JUnit 5] --> AA[Test Coverage]
        BB[Testcontainers] --> CC[Integration Tests]
    end
    
    B --> G
    B --> K
    B --> O
    B --> S
    B --> X
```

This visual documentation provides comprehensive diagrams and mind maps to help understand the system architecture, data flow, and technology stack of the Online Bookstore application.
