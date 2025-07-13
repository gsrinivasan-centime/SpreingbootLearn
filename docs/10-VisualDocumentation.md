# Visual Architecture Documentation

## 1. System Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Client/Postman]
        MOB[Mobile App]
        API_GW[API Gateway/Load Balancer]
    end
    
    subgraph "Microservices Layer"
        subgraph "Book Service (Port 8081)"
            BS_CTRL[BookController]
            BS_SVC[BookService]
            BS_REPO[BookRepository]
        end
        
        subgraph "User Service (Port 8082)"
            US_CTRL[UserController]
            US_SVC[UserService]
            US_REPO[UserRepository]
        end
    end
    
    subgraph "Infrastructure Layer"
        subgraph "Databases"
            MYSQL[(MySQL 8.0)]
            REDIS[(Redis Cache)]
        end
        
        subgraph "Message Broker"
            ZK[Zookeeper]
            KAFKA[Kafka]
        end
        
        subgraph "Monitoring & Admin"
            KAFKA_UI[Kafka UI<br/>Port 8080]
            REDIS_CMD[Redis Commander<br/>Port 8090]
        end
    end
    
    subgraph "Container Platform"
        DOCKER[Docker<br/>Docker Compose]
    end
    
    %% Client connections
    WEB --> API_GW
    MOB --> API_GW
    API_GW --> BS_CTRL
    API_GW --> US_CTRL
    
    %% Service layer connections
    BS_CTRL --> BS_SVC
    BS_SVC --> BS_REPO
    US_CTRL --> US_SVC
    US_SVC --> US_REPO
    
    %% Database connections
    BS_REPO --> MYSQL
    US_REPO --> MYSQL
    BS_SVC --> REDIS
    US_SVC --> REDIS
    
    %% Messaging connections
    BS_SVC --> KAFKA
    US_SVC --> KAFKA
    KAFKA --> ZK
    
    %% Container orchestration
    DOCKER -.-> BS_CTRL
    DOCKER -.-> US_CTRL
    DOCKER -.-> MYSQL
    DOCKER -.-> REDIS
    DOCKER -.-> KAFKA
    DOCKER -.-> ZK
    
    %% Admin interfaces
    KAFKA_UI --> KAFKA
    REDIS_CMD --> REDIS
    
    style BS_CTRL fill:#e1f5fe
    style US_CTRL fill:#e8f5e8
    style MYSQL fill:#fff3e0
    style REDIS fill:#fce4ec
    style KAFKA fill:#f3e5f5
```

## 2. Data Flow Architecture

```mermaid
sequenceDiagram
    participant Client
    participant BookService
    participant UserService
    participant MySQL
    participant Redis
    participant Kafka
    
    Note over Client,Kafka: User Registration Flow
    Client->>UserService: POST /api/v1/users
    UserService->>MySQL: Save user data
    UserService->>Redis: Cache user session
    UserService->>Kafka: Publish UserCreated event
    UserService-->>Client: Return user details
    
    Note over Client,Kafka: Book Creation Flow
    Client->>BookService: POST /api/v1/books
    BookService->>MySQL: Save book data
    BookService->>Redis: Cache book data
    BookService->>Kafka: Publish BookCreated event
    BookService-->>Client: Return book details
    
    Note over Client,Kafka: Book Search Flow (Cached)
    Client->>BookService: GET /api/v1/books/{id}
    BookService->>Redis: Check cache
    alt Cache Hit
        Redis-->>BookService: Return cached data
        BookService-->>Client: Return book details
    else Cache Miss
        BookService->>MySQL: Query database
        MySQL-->>BookService: Return book data
        BookService->>Redis: Update cache
        BookService-->>Client: Return book details
    end
```

## 3. Container Architecture

```mermaid
graph TB
    subgraph "Docker Host"
        subgraph "Application Containers"
            BS_CONTAINER[book-service:latest<br/>Port 8081<br/>Java 17 + Spring Boot]
            US_CONTAINER[user-service:latest<br/>Port 8082<br/>Java 17 + Spring Boot]
        end
        
        subgraph "Infrastructure Containers"
            MYSQL_CONTAINER[mysql:8.0<br/>Port 3308<br/>Database Storage]
            REDIS_CONTAINER[redis:7-alpine<br/>Port 6379<br/>Caching Layer]
            KAFKA_CONTAINER[confluentinc/cp-kafka:7.4.0<br/>Port 9092<br/>Message Broker]
            ZK_CONTAINER[confluentinc/cp-zookeeper:7.4.0<br/>Port 2181<br/>Kafka Coordination]
        end
        
        subgraph "Management Containers"
            KAFKA_UI_CONTAINER[provectuslabs/kafka-ui<br/>Port 8080<br/>Kafka Management]
            REDIS_UI_CONTAINER[rediscommander/redis-commander<br/>Port 8090<br/>Redis Management]
        end
        
        subgraph "Volumes"
            MYSQL_VOL[mysql_data]
            REDIS_VOL[redis_data]
        end
        
        subgraph "Networks"
            NETWORK[bookstore-network<br/>Bridge Network]
        end
    end
    
    %% Container connections
    BS_CONTAINER -.-> NETWORK
    US_CONTAINER -.-> NETWORK
    MYSQL_CONTAINER -.-> NETWORK
    REDIS_CONTAINER -.-> NETWORK
    KAFKA_CONTAINER -.-> NETWORK
    ZK_CONTAINER -.-> NETWORK
    KAFKA_UI_CONTAINER -.-> NETWORK
    REDIS_UI_CONTAINER -.-> NETWORK
    
    %% Volume mounts
    MYSQL_CONTAINER --> MYSQL_VOL
    REDIS_CONTAINER --> REDIS_VOL
    
    style BS_CONTAINER fill:#e1f5fe
    style US_CONTAINER fill:#e8f5e8
    style MYSQL_CONTAINER fill:#fff3e0
    style REDIS_CONTAINER fill:#fce4ec
    style KAFKA_CONTAINER fill:#f3e5f5
    style NETWORK fill:#f5f5f5
```

## 4. Book Service - UML Class Diagram

```mermaid
classDiagram
    class BookServiceApplication {
        +main(String[] args)
    }
    
    class BookController {
        -BookService bookService
        +getAllBooks(page, size) ResponseEntity~List~BookDto~~
        +getBookById(id) ResponseEntity~BookDto~
        +createBook(request) ResponseEntity~BookDto~
        +updateBook(id, request) ResponseEntity~BookDto~
        +deleteBook(id) ResponseEntity~Void~
        +searchBooks(criteria) ResponseEntity~List~BookDto~~
        +getBooksInBatch(ids) ResponseEntity~List~BookDto~~
    }
    
    class BookService {
        <<interface>>
        +getAllBooks(page, size) List~BookDto~
        +getBookById(id) BookDto
        +createBook(request) BookDto
        +updateBook(id, request) BookDto
        +deleteBook(id) void
        +searchBooks(criteria) List~BookDto~
        +getBooksInBatch(ids) List~BookDto~
    }
    
    class BookServiceImpl {
        -BookRepository bookRepository
        -RedisTemplate redisTemplate
        -KafkaProducerService kafkaProducerService
        -IdempotencyService idempotencyService
        +getAllBooks(page, size) List~BookDto~
        +getBookById(id) BookDto
        +createBook(request) BookDto
        +updateBook(id, request) BookDto
        +deleteBook(id) void
        -validateBookData(book) void
        -publishBookEvent(event) void
    }
    
    class BookRepository {
        <<interface>>
        +findById(id) Optional~Book~
        +findAll(pageable) Page~Book~
        +findByTitleContainingIgnoreCase(title) List~Book~
        +findByAuthorContainingIgnoreCase(author) List~Book~
        +findByCategoryAndAvailableTrue(category) List~Book~
        +findByIsbn(isbn) Optional~Book~
        +updateStockById(id, stock) int
    }
    
    class Book {
        -Long id
        -String title
        -String author
        -String isbn
        -String category
        -BigDecimal price
        -Integer stock
        -String description
        -Boolean available
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +getId() Long
        +setId(Long) void
        +getTitle() String
        +setTitle(String) void
        +equals(Object) boolean
        +hashCode() int
        +toString() String
    }
    
    class BookDto {
        -Long id
        -String title
        -String author
        -String isbn
        -String category
        -BigDecimal price
        -Integer stock
        -String description
        -Boolean available
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }
    
    class CreateBookRequestDto {
        -String title
        -String author
        -String isbn
        -String category
        -BigDecimal price
        -Integer stock
        -String description
    }
    
    class UpdateBookRequestDto {
        -String title
        -String author
        -String category
        -BigDecimal price
        -Integer stock
        -String description
        -Boolean available
    }
    
    class BookEvent {
        -String eventType
        -Long bookId
        -BookDto bookData
        -LocalDateTime timestamp
        -String correlationId
    }
    
    class KafkaProducerService {
        -KafkaTemplate kafkaTemplate
        +publishBookEvent(event) void
        +publishBookCreated(bookId) void
        +publishBookUpdated(bookId) void
        +publishBookDeleted(bookId) void
    }
    
    class IdempotencyService {
        -RedisTemplate redisTemplate
        +isOperationProcessed(key) boolean
        +markOperationAsProcessed(key) void
        +generateIdempotencyKey(operation, params) String
    }
    
    class GlobalExceptionHandler {
        +handleBookNotFound(ex) ResponseEntity~ErrorResponse~
        +handleDuplicateIsbn(ex) ResponseEntity~ErrorResponse~
        +handleValidationException(ex) ResponseEntity~ErrorResponse~
        +handleGenericException(ex) ResponseEntity~ErrorResponse~
    }
    
    class RedisConfig {
        +redisTemplate() RedisTemplate
        +cacheManager() CacheManager
    }
    
    class KafkaConfig {
        +kafkaTemplate() KafkaTemplate
        +producerFactory() ProducerFactory
    }
    
    %% Relationships
    BookController --> BookService : uses
    BookServiceImpl ..|> BookService : implements
    BookServiceImpl --> BookRepository : uses
    BookServiceImpl --> KafkaProducerService : uses
    BookServiceImpl --> IdempotencyService : uses
    BookRepository --> Book : manages
    BookServiceImpl --> BookDto : creates
    BookController --> CreateBookRequestDto : receives
    BookController --> UpdateBookRequestDto : receives
    BookController --> BookDto : returns
    KafkaProducerService --> BookEvent : publishes
    BookServiceImpl --> Book : works with
    
    %% Styling
    class BookController {
        <<@RestController>>
    }
    class BookServiceImpl {
        <<@Service>>
    }
    class BookRepository {
        <<@Repository>>
    }
    class Book {
        <<@Entity>>
    }
    class RedisConfig {
        <<@Configuration>>
    }
    class KafkaConfig {
        <<@Configuration>>
    }
```

## 5. User Service - UML Class Diagram

```mermaid
classDiagram
    class UserServiceApplication {
        +main(String[] args)
    }
    
    class UserController {
        -UserService userService
        +getAllUsers(page, size) ResponseEntity~List~UserDto~~
        +getUserById(id) ResponseEntity~UserDto~
        +createUser(request) ResponseEntity~UserDto~
        +updateUser(id, request) ResponseEntity~UserDto~
        +deleteUser(id) ResponseEntity~Void~
        +searchUsers(criteria) ResponseEntity~List~UserDto~~
        +getUsersInBatch(ids) ResponseEntity~List~UserDto~~
    }
    
    class UserService {
        <<interface>>
        +getAllUsers(page, size) List~UserDto~
        +getUserById(id) UserDto
        +createUser(request) UserDto
        +updateUser(id, request) UserDto
        +deleteUser(id) void
        +searchUsers(criteria) List~UserDto~
        +getUsersInBatch(ids) List~UserDto~
    }
    
    class UserServiceImpl {
        -UserRepository userRepository
        -RedisTemplate redisTemplate
        -KafkaProducerService kafkaProducerService
        -IdempotencyService idempotencyService
        -PasswordEncoder passwordEncoder
        +getAllUsers(page, size) List~UserDto~
        +getUserById(id) UserDto
        +createUser(request) UserDto
        +updateUser(id, request) UserDto
        +deleteUser(id) void
        -validateUserData(user) void
        -encryptSensitiveData(user) void
        -publishUserEvent(event) void
    }
    
    class UserRepository {
        <<interface>>
        +findById(id) Optional~User~
        +findAll(pageable) Page~User~
        +findByEmail(email) Optional~User~
        +findByUsername(username) Optional~User~
        +findByPhoneNumber(phone) Optional~User~
        +findByActiveTrue() List~User~
        +updateLastLoginById(id, timestamp) int
    }
    
    class User {
        -Long id
        -String username
        -String email
        -String encryptedPassword
        -String firstName
        -String lastName
        -String phoneNumber
        -LocalDate dateOfBirth
        -String address
        -String city
        -String country
        -String postalCode
        -Boolean active
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -LocalDateTime lastLogin
        +getId() Long
        +setId(Long) void
        +getUsername() String
        +setUsername(String) void
        +equals(Object) boolean
        +hashCode() int
        +toString() String
    }
    
    class UserDto {
        -Long id
        -String username
        -String email
        -String firstName
        -String lastName
        -String phoneNumber
        -LocalDate dateOfBirth
        -String address
        -String city
        -String country
        -String postalCode
        -Boolean active
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -LocalDateTime lastLogin
    }
    
    class CreateUserRequestDto {
        -String username
        -String email
        -String password
        -String firstName
        -String lastName
        -String phoneNumber
        -LocalDate dateOfBirth
        -String address
        -String city
        -String country
        -String postalCode
    }
    
    class UpdateUserRequestDto {
        -String email
        -String firstName
        -String lastName
        -String phoneNumber
        -LocalDate dateOfBirth
        -String address
        -String city
        -String country
        -String postalCode
        -Boolean active
    }
    
    class UserEvent {
        -String eventType
        -Long userId
        -UserDto userData
        -LocalDateTime timestamp
        -String correlationId
    }
    
    class UserMapper {
        +toDto(user) UserDto
        +toEntity(dto) User
        +toEntity(request) User
        +updateEntityFromDto(dto, entity) void
    }
    
    class KafkaProducerService {
        -KafkaTemplate kafkaTemplate
        +publishUserEvent(event) void
        +publishUserCreated(userId) void
        +publishUserUpdated(userId) void
        +publishUserDeleted(userId) void
    }
    
    class IdempotencyService {
        -RedisTemplate redisTemplate
        +isOperationProcessed(key) boolean
        +markOperationAsProcessed(key) void
        +generateIdempotencyKey(operation, params) String
    }
    
    class PhoneNumberConverter {
        +convertToDatabaseColumn(phone) String
        +convertToEntityAttribute(dbData) String
        -encryptPhoneNumber(phone) String
        -decryptPhoneNumber(encrypted) String
    }
    
    class GlobalExceptionHandler {
        +handleUserNotFound(ex) ResponseEntity~ErrorResponse~
        +handleDuplicateEmail(ex) ResponseEntity~ErrorResponse~
        +handleDuplicatePhone(ex) ResponseEntity~ErrorResponse~
        +handleValidationException(ex) ResponseEntity~ErrorResponse~
        +handleGenericException(ex) ResponseEntity~ErrorResponse~
    }
    
    class EncryptionConfig {
        +passwordEncoder() PasswordEncoder
        +aesUtil() AESUtil
    }
    
    class RedisConfig {
        +redisTemplate() RedisTemplate
        +cacheManager() CacheManager
    }
    
    class KafkaConfig {
        +kafkaTemplate() KafkaTemplate
        +producerFactory() ProducerFactory
    }
    
    %% Relationships
    UserController --> UserService : uses
    UserServiceImpl ..|> UserService : implements
    UserServiceImpl --> UserRepository : uses
    UserServiceImpl --> KafkaProducerService : uses
    UserServiceImpl --> IdempotencyService : uses
    UserServiceImpl --> UserMapper : uses
    UserRepository --> User : manages
    UserServiceImpl --> UserDto : creates
    UserController --> CreateUserRequestDto : receives
    UserController --> UpdateUserRequestDto : receives
    UserController --> UserDto : returns
    KafkaProducerService --> UserEvent : publishes
    UserServiceImpl --> User : works with
    User --> PhoneNumberConverter : uses
    UserMapper --> User : maps
    UserMapper --> UserDto : maps
    
    %% Styling
    class UserController {
        <<@RestController>>
    }
    class UserServiceImpl {
        <<@Service>>
    }
    class UserRepository {
        <<@Repository>>
    }
    class User {
        <<@Entity>>
    }
    class PhoneNumberConverter {
        <<@Converter>>
    }
    class EncryptionConfig {
        <<@Configuration>>
    }
    class RedisConfig {
        <<@Configuration>>
    }
    class KafkaConfig {
        <<@Configuration>>
    }
```

## 6. Database Schema Diagram

```mermaid
erDiagram
    BOOKS {
        bigint id PK "AUTO_INCREMENT"
        varchar(255) title "NOT NULL"
        varchar(255) author "NOT NULL"
        varchar(20) isbn "UNIQUE NOT NULL"
        varchar(100) category
        decimal(10,2) price "NOT NULL"
        int stock "DEFAULT 0"
        text description
        boolean available "DEFAULT true"
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    }
    
    USERS {
        bigint id PK "AUTO_INCREMENT"
        varchar(50) username "UNIQUE NOT NULL"
        varchar(255) email "UNIQUE NOT NULL"
        varchar(255) encrypted_password "NOT NULL"
        varchar(100) first_name
        varchar(100) last_name
        varchar(20) phone_number "UNIQUE"
        date date_of_birth
        text address
        varchar(100) city
        varchar(100) country
        varchar(20) postal_code
        boolean active "DEFAULT true"
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
        timestamp last_login
    }
    
    %% Indexes
    BOOKS }|--|| IDX_BOOK_ISBN : "UNIQUE INDEX"
    BOOKS }|--|| IDX_BOOK_TITLE : "INDEX"
    BOOKS }|--|| IDX_BOOK_AUTHOR : "INDEX"
    BOOKS }|--|| IDX_BOOK_CATEGORY : "INDEX"
    
    USERS }|--|| IDX_USER_EMAIL : "UNIQUE INDEX"
    USERS }|--|| IDX_USER_USERNAME : "UNIQUE INDEX"
    USERS }|--|| IDX_USER_ACTIVE : "INDEX"
    USERS }|--|| IDX_USER_CREATED_AT : "INDEX"
```

## 7. Kafka Event Flow

```mermaid
graph LR
    subgraph "Event Producers"
        BS[Book Service]
        US[User Service]
    end
    
    subgraph "Kafka Topics"
        BT[book-events]
        UT[user-events]
    end
    
    subgraph "Event Consumers"
        ES[Email Service]
        NS[Notification Service]
        AS[Analytics Service]
        RS[Recommendation Service]
    end
    
    %% Event publishing
    BS -->|BookCreated<br/>BookUpdated<br/>BookDeleted| BT
    US -->|UserCreated<br/>UserUpdated<br/>UserDeleted| UT
    
    %% Event consumption
    BT --> ES
    BT --> NS
    BT --> AS
    BT --> RS
    
    UT --> ES
    UT --> NS
    UT --> AS
    UT --> RS
    
    style BS fill:#e1f5fe
    style US fill:#e8f5e8
    style BT fill:#f3e5f5
    style UT fill:#f3e5f5
    style ES fill:#fff3e0
    style NS fill:#fff3e0
    style AS fill:#fff3e0
    style RS fill:#fff3e0
```

## 8. Redis Caching Strategy

```mermaid
graph TB
    subgraph "Application Layer"
        BS[Book Service]
        US[User Service]
    end
    
    subgraph "Redis Cache"
        subgraph "Cache Namespaces"
            BC[book:*<br/>TTL: 1 hour]
            UC[user:*<br/>TTL: 30 minutes]
            SC[session:*<br/>TTL: 24 hours]
            IC[idempotency:*<br/>TTL: 5 minutes]
        end
    end
    
    subgraph "Cache Patterns"
        CT[Cache-Through]
        CA[Cache-Aside]
        WB[Write-Behind]
    end
    
    %% Service to cache connections
    BS --> BC
    BS --> IC
    US --> UC
    US --> SC
    US --> IC
    
    %% Cache patterns
    BC -.-> CT
    UC -.-> CA
    SC -.-> WB
    IC -.-> CA
    
    style BS fill:#e1f5fe
    style US fill:#e8f5e8
    style BC fill:#fce4ec
    style UC fill:#fce4ec
    style SC fill:#fce4ec
    style IC fill:#fce4ec
```

## 9. Deployment Flow

```mermaid
graph TB
    START([Start Deployment])
    
    subgraph "Build Phase"
        B1[Build Book Service JAR]
        B2[Build User Service JAR]
        B3[Create Docker Images]
    end
    
    subgraph "Infrastructure Phase"
        I1[Start MySQL Container]
        I2[Start Redis Container]
        I3[Start Zookeeper Container]
        I4[Start Kafka Container]
        I5[Start Management UIs]
        WAIT[Wait for Health Checks]
    end
    
    subgraph "Application Phase"
        A1[Deploy Book Service]
        A2[Deploy User Service]
        A3[Run Health Checks]
    end
    
    subgraph "Verification Phase"
        V1[Test API Endpoints]
        V2[Verify Database Connectivity]
        V3[Check Cache Functionality]
        V4[Validate Message Publishing]
    end
    
    END([Deployment Complete])
    
    START --> B1
    B1 --> B2
    B2 --> B3
    B3 --> I1
    I1 --> I2
    I2 --> I3
    I3 --> I4
    I4 --> I5
    I5 --> WAIT
    WAIT --> A1
    A1 --> A2
    A2 --> A3
    A3 --> V1
    V1 --> V2
    V2 --> V3
    V3 --> V4
    V4 --> END
    
    style START fill:#c8e6c9
    style END fill:#c8e6c9
    style B1 fill:#e1f5fe
    style B2 fill:#e8f5e8
    style I1 fill:#fff3e0
    style I2 fill:#fce4ec
    style I3 fill:#f3e5f5
    style I4 fill:#f3e5f5
```

## 10. API Endpoint Overview

### Book Service Endpoints (Port 8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/books` | Get all books (paginated) |
| GET | `/api/v1/books/{id}` | Get book by ID |
| POST | `/api/v1/books` | Create new book |
| PUT | `/api/v1/books/{id}` | Update book |
| DELETE | `/api/v1/books/{id}` | Delete book |
| GET | `/api/v1/books/search` | Search books |
| POST | `/api/v1/books/batch` | Get books in batch |
| GET | `/api/v1/actuator/health` | Health check |
| GET | `/api/v1/swagger-ui.html` | API documentation |

### User Service Endpoints (Port 8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/users` | Get all users (paginated) |
| GET | `/api/v1/users/{id}` | Get user by ID |
| POST | `/api/v1/users` | Create new user |
| PUT | `/api/v1/users/{id}` | Update user |
| DELETE | `/api/v1/users/{id}` | Delete user |
| GET | `/api/v1/users/search` | Search users |
| POST | `/api/v1/users/batch` | Get users in batch |
| GET | `/api/v1/actuator/health` | Health check |
| GET | `/api/v1/swagger-ui.html` | API documentation |

## 11. Technology Stack Summary

```mermaid
graph TB
    subgraph "Application Layer"
        JAVA[Java 17]
        SB[Spring Boot 3.2]
        SW[Spring Web]
        SD[Spring Data JPA]
        SC[Spring Cache]
        SK[Spring Kafka]
    end
    
    subgraph "Database Layer"
        MYSQL[MySQL 8.0]
        REDIS[Redis 7]
        HIB[Hibernate ORM]
    end
    
    subgraph "Messaging Layer"
        KAFKA[Apache Kafka 7.4]
        ZK[Zookeeper]
    end
    
    subgraph "Container Layer"
        DOCKER[Docker]
        DC[Docker Compose]
    end
    
    subgraph "Documentation & Testing"
        SWAGGER[OpenAPI/Swagger]
        JUNIT[JUnit 5]
        MOCK[Mockito]
        TEST[TestContainers]
    end
    
    subgraph "Monitoring & Management"
        ACT[Spring Actuator]
        KUI[Kafka UI]
        RC[Redis Commander]
    end
    
    %% Dependencies
    SB --> JAVA
    SW --> SB
    SD --> SB
    SC --> SB
    SK --> SB
    HIB --> SD
    HIB --> MYSQL
    SC --> REDIS
    SK --> KAFKA
    KAFKA --> ZK
    
    style JAVA fill:#f4511e
    style SB fill:#6db33f
    style MYSQL fill:#4479a1
    style REDIS fill:#dc382d
    style KAFKA fill:#231f20
    style DOCKER fill:#2496ed
```

This comprehensive visual documentation provides:

1. **System Architecture Overview** - Complete microservices architecture
2. **Data Flow Architecture** - Sequence diagrams showing request flows
3. **Container Architecture** - Docker container relationships
4. **UML Class Diagrams** - Detailed class structures for both services
5. **Database Schema** - Entity relationship diagrams
6. **Kafka Event Flow** - Message broker event patterns
7. **Redis Caching Strategy** - Caching patterns and TTL strategies
8. **Deployment Flow** - Step-by-step deployment process
9. **API Endpoint Overview** - Complete REST API reference
10. **Technology Stack Summary** - All technologies and their relationships

These diagrams will help you understand the complete architecture and explain the microservices pattern effectively for learning and interviews.
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
