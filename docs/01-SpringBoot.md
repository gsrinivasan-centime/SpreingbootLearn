# Spring Boot Fundamentals

## What is Spring Boot?

Spring Boot is a framework that simplifies the development of Java applications by providing:
- **Auto-configuration**: Automatically configures Spring applications based on dependencies
- **Starter dependencies**: Pre-configured dependency sets
- **Embedded servers**: Built-in Tomcat, Jetty, or Undertow
- **Production-ready features**: Health checks, metrics, externalized configuration

## Key Concepts for Interview

### 1. Spring Boot Architecture

```
┌─────────────────────────────────────┐
│          Application Layer          │
│  (Controllers, Services, Repos)     │
├─────────────────────────────────────┤
│         Spring Boot Framework       │
│  (Auto-config, Starters, Actuator)  │
├─────────────────────────────────────┤
│           Spring Framework          │
│     (IoC, AOP, Data Access)         │
├─────────────────────────────────────┤
│              JVM/JRE                │
└─────────────────────────────────────┘
```

### 2. Core Annotations

| Annotation | Purpose | Example Usage |
|------------|---------|---------------|
| `@SpringBootApplication` | Main application class | Entry point |
| `@RestController` | REST API controller | HTTP endpoints |
| `@Service` | Business logic layer | Service classes |
| `@Repository` | Data access layer | Database operations |
| `@Component` | Generic Spring bean | Utility classes |
| `@Configuration` | Configuration class | Bean definitions |
| `@Autowired` | Dependency injection | Inject dependencies |
| `@Value` | Property injection | Externalized config |

### 3. Application Layers in Our Bookstore

#### Controller Layer (Presentation)
```java
@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {
    
    @Autowired
    private BookService bookService;
    
    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookService.getAllBooks(page, size));
    }
    
    @PostMapping
    @Valid
    public ResponseEntity<BookDTO> createBook(@RequestBody @Valid CreateBookRequest request) {
        BookDTO book = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }
}
```

#### Service Layer (Business Logic)
```java
@Service
@Transactional
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "books", key = "#id")
    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
        return BookMapper.toDTO(book);
    }
}
```

#### Repository Layer (Data Access)
```java
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    @Query("SELECT b FROM Book b WHERE b.category = :category AND b.available = true")
    List<Book> findAvailableBooksByCategory(@Param("category") String category);
    
    @Modifying
    @Query("UPDATE Book b SET b.stock = b.stock - :quantity WHERE b.id = :id")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
```

### 4. Configuration Management

#### application.yml Structure
```yaml
# Development Profile
spring:
  profiles:
    active: dev
  application:
    name: book-service
  
  # Database Configuration
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore_books
    username: ${DB_USERNAME:bookstore_user}
    password: ${DB_PASSWORD:password123}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  # JPA/Hibernate Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        
  # Redis Configuration
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    
  # Kafka Configuration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: book-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

# Server Configuration
server:
  port: 8081
  servlet:
    context-path: /api/v1

# Management & Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 5. Auto-Configuration Magic

Spring Boot automatically configures beans based on:

1. **Classpath Dependencies**: If JPA is on classpath → DataSource beans created
2. **Property Values**: If `spring.datasource.url` exists → Database connection configured
3. **Bean Presence**: If no custom DataSource → Default one created

#### Example Auto-Configuration Flow
```java
@Configuration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(name = "spring.datasource.url")
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

### 6. Bean Lifecycle and Scopes

#### Bean Scopes
- **Singleton** (default): One instance per Spring container
- **Prototype**: New instance every time
- **Request**: One instance per HTTP request
- **Session**: One instance per HTTP session

#### Lifecycle Callbacks
```java
@Component
public class BookCacheManager implements InitializingBean, DisposableBean {
    
    @PostConstruct
    public void init() {
        // Initialize cache
        log.info("Initializing book cache...");
    }
    
    @PreDestroy
    public void cleanup() {
        // Cleanup resources
        log.info("Cleaning up book cache...");
    }
    
    @Override
    public void afterPropertiesSet() {
        // Custom initialization logic
    }
    
    @Override
    public void destroy() {
        // Custom cleanup logic
    }
}
```

### 7. Exception Handling

#### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Book Not Found")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        // Handle validation errors
    }
}
```

### 8. Profiles and Environment-Specific Configuration

#### Profile-Specific Files
- `application.yml` - Default configuration
- `application-dev.yml` - Development environment
- `application-prod.yml` - Production environment
- `application-test.yml` - Testing environment

#### Conditional Configuration
```java
@Configuration
@Profile("!test")
public class ProductionConfig {
    
    @Bean
    @ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new RedisCacheManager.Builder(redisConnectionFactory()).build();
    }
}
```

## Interview Questions & Answers

### Q1: What is the difference between @Component, @Service, @Repository, and @Controller?

**Answer**: These are all specializations of @Component:
- **@Component**: Generic stereotype for any Spring-managed component
- **@Service**: Business logic layer, indicates service class
- **@Repository**: Data access layer, provides exception translation
- **@Controller**: Presentation layer, handles HTTP requests

### Q2: How does Spring Boot auto-configuration work?

**Answer**: Auto-configuration uses:
1. **@ConditionalOn...** annotations to check conditions
2. **Classpath scanning** to detect dependencies
3. **Property files** to enable/disable features
4. **META-INF/spring.factories** to register auto-configuration classes

### Q3: What is the difference between @Autowired and @Inject?

**Answer**: 
- **@Autowired**: Spring-specific, supports required=false
- **@Inject**: JSR-330 standard, doesn't have required attribute
- Both support field, setter, and constructor injection

### Q4: How do you handle circular dependencies?

**Answer**: 
1. **Redesign**: Refactor to eliminate circular dependency
2. **@Lazy**: Lazy initialization of one bean
3. **Setter injection**: Instead of constructor injection
4. **@PostConstruct**: Initialize after bean creation

## Best Practices

1. **Use Constructor Injection** for required dependencies
2. **Prefer @ConfigurationProperties** over @Value for complex configuration
3. **Use profiles** for environment-specific configuration
4. **Implement proper exception handling** with @ControllerAdvice
5. **Use validation annotations** for input validation
6. **Enable actuator endpoints** for monitoring
7. **Use proper logging** with SLF4J and Logback
8. **Write tests** for all layers

## Next Steps

Continue to [Hibernate & JPA](02-Hibernate.md) to learn about database operations.
