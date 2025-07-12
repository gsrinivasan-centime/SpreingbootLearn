# Testing in Spring Boot Microservices

## Overview
This document covers comprehensive testing strategies for our Spring Boot microservices, including unit tests, integration tests, contract testing, and SonarQube compliance.

## Table of Contents
1. [Testing Pyramid](#testing-pyramid)
2. [Unit Testing](#unit-testing)
3. [Integration Testing](#integration-testing)
4. [Contract Testing](#contract-testing)
5. [Test Containers](#test-containers)
6. [SonarQube Integration](#sonarqube-integration)
7. [Testing Best Practices](#testing-best-practices)
8. [Code Coverage](#code-coverage)
9. [Performance Testing](#performance-testing)
10. [Interview Questions](#interview-questions)

## Testing Pyramid

### 1. Unit Tests (70%)
- Fast execution
- Test individual components in isolation
- Mock external dependencies
- High code coverage

### 2. Integration Tests (20%)
- Test component interactions
- Use real databases and message queues
- Test containers for isolation

### 3. End-to-End Tests (10%)
- Test complete user journeys
- Test entire application stack
- Slower but comprehensive

## Unit Testing

### Service Layer Testing

```java
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void createBook_Success() {
        // Arrange
        CreateBookRequestDto request = new CreateBookRequestDto();
        request.setTitle("Test Book");
        request.setIsbn("9781234567890");
        
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        
        when(bookRepository.existsByIsbn(request.getIsbn())).thenReturn(false);
        when(bookMapper.toEntity(request)).thenReturn(book);
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.toDto(book)).thenReturn(new BookDto());

        // Act
        BookDto result = bookService.createBook(request);

        // Assert
        assertNotNull(result);
        verify(bookRepository).save(book);
        verify(kafkaProducerService).publishBookEvent(any());
    }

    @Test
    void createBook_DuplicateIsbn_ThrowsException() {
        // Arrange
        CreateBookRequestDto request = new CreateBookRequestDto();
        request.setIsbn("9781234567890");
        
        when(bookRepository.existsByIsbn(request.getIsbn())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateIsbnException.class, () -> {
            bookService.createBook(request);
        });
    }
}
```

### Repository Testing

```java
@DataJpaTest
@Testcontainers
class BookRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_books")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void findByIsbn_Success() {
        // Arrange
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setIsbn("9781234567890");
        book.setPrice(new BigDecimal("19.99"));
        book.setStockQuantity(50);
        book.setCategory("Fiction");
        
        entityManager.persistAndFlush(book);

        // Act
        Optional<Book> result = bookRepository.findByIsbn("9781234567890");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Book", result.get().getTitle());
    }

    @Test
    void findBooksWithLowStock_Success() {
        // Arrange
        Book lowStockBook = new Book();
        lowStockBook.setTitle("Low Stock Book");
        lowStockBook.setStockQuantity(5);
        // ... set other required fields
        
        Book highStockBook = new Book();
        highStockBook.setTitle("High Stock Book");
        highStockBook.setStockQuantity(100);
        // ... set other required fields
        
        entityManager.persistAndFlush(lowStockBook);
        entityManager.persistAndFlush(highStockBook);

        // Act
        List<Book> result = bookRepository.findBooksWithLowStock(10);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Low Stock Book", result.get(0).getTitle());
    }
}
```

### Controller Testing

```java
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createBook_Success() throws Exception {
        // Arrange
        CreateBookRequestDto request = new CreateBookRequestDto();
        request.setTitle("Test Book");
        request.setAuthor("Test Author");
        request.setIsbn("9781234567890");
        request.setPrice(new BigDecimal("19.99"));
        request.setStockQuantity(50);
        request.setCategory("Fiction");

        BookDto response = new BookDto();
        response.setId(1L);
        response.setTitle("Test Book");

        when(bookService.createBook(any(CreateBookRequestDto.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    void createBook_ValidationError() throws Exception {
        // Arrange
        CreateBookRequestDto request = new CreateBookRequestDto();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

## Integration Testing

### Full Application Context Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
public class BookServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("bookstore_books_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void createAndRetrieveBook_Success() {
        // Arrange
        CreateBookRequestDto request = new CreateBookRequestDto();
        request.setTitle("Integration Test Book");
        request.setAuthor("Test Author");
        request.setIsbn("9781234567890");
        request.setPrice(new BigDecimal("29.99"));
        request.setStockQuantity(100);
        request.setCategory("Technology");

        // Act - Create book
        ResponseEntity<BookDto> createResponse = restTemplate.postForEntity(
            "/api/v1/books", request, BookDto.class);

        // Assert - Creation successful
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Long bookId = createResponse.getBody().getId();

        // Act - Retrieve book
        ResponseEntity<BookDto> getResponse = restTemplate.getForEntity(
            "/api/v1/books/" + bookId, BookDto.class);

        // Assert - Retrieval successful
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Integration Test Book", getResponse.getBody().getTitle());
    }
}
```

### Cache Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=simple" // Use simple cache for testing
})
class CacheIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void bookCache_WorksCorrectly() {
        // First call - should hit database
        BookDto book1 = bookService.getBookById(1L);
        
        // Verify cache entry exists
        Cache bookCache = cacheManager.getCache("books");
        assertNotNull(bookCache.get(1L));
        
        // Second call - should hit cache
        BookDto book2 = bookService.getBookById(1L);
        
        // Should be same instance if properly cached
        assertEquals(book1.getTitle(), book2.getTitle());
    }
}
```

## Contract Testing

### Spring Cloud Contract

#### Contract Definition (Groovy DSL)

```groovy
// contracts/book_service_get_book.groovy
Contract.make {
    description "should return book by id"
    request {
        method GET()
        url "/api/v1/books/1"
        headers {
            contentType(applicationJson())
        }
    }
    response {
        status OK()
        body([
            id: 1,
            title: "Test Book",
            author: "Test Author",
            isbn: "9781234567890",
            price: 19.99,
            stockQuantity: 50,
            category: "Fiction",
            active: true
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
```

#### Producer Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureStubRunner(
    ids = "com.bookstore:user-service:+:stubs:8082",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class BookServiceContractTest extends BookServiceApplicationTests {

    @MockBean
    private BookService bookService;

    @BeforeEach
    void setUp() {
        BookDto testBook = new BookDto();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("9781234567890");
        testBook.setPrice(new BigDecimal("19.99"));
        testBook.setStockQuantity(50);
        testBook.setCategory("Fiction");
        testBook.setActive(true);

        when(bookService.getBookById(1L)).thenReturn(testBook);
    }
}
```

#### Consumer Test

```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.bookstore:book-service:+:stubs:8081",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class UserServiceContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldGetBookFromBookService() {
        // Act
        ResponseEntity<BookDto> response = restTemplate.getForEntity(
            "http://localhost:8081/api/v1/books/1", BookDto.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test Book", response.getBody().getTitle());
    }
}
```

## Test Containers

### Database Testing

```java
@Testcontainers
public abstract class DatabaseTestBase {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withCommand("--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
```

### Kafka Testing

```java
@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "book-events", groupId = "test-group")
    void handleBookEvent(BookEvent event) {
        receivedEvents.add(event);
    }

    @Test
    void shouldPublishAndConsumeBookEvent() {
        // Arrange
        BookEvent event = new BookEvent("BOOK_CREATED", 1L, "Test Book", "Test Author", "123456", 50);

        // Act
        kafkaTemplate.send("book-events", event);

        // Assert
        await().atMost(Duration.ofSeconds(10))
               .until(() -> receivedEvents.size() == 1);
        
        assertEquals("BOOK_CREATED", receivedEvents.get(0).getEventType());
    }
}
```

## SonarQube Integration

### Maven Configuration

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>

<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### SonarQube Properties

```properties
# sonar-project.properties
sonar.projectKey=bookstore-microservices
sonar.projectName=Bookstore Microservices
sonar.projectVersion=1.0.0

sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes

sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.junit.reportPaths=target/surefire-reports

# Quality Gates
sonar.coverage.exclusions=**/*Application.java,**/*Config.java,**/dto/**,**/entity/**
sonar.cpd.exclusions=**/*Test.java
```

### Running SonarQube Analysis

```bash
# Start SonarQube (using Docker)
docker run -d --name sonarqube -p 9000:9000 sonarqube:latest

# Run analysis
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin
```

## Testing Best Practices

### 1. Test Naming Convention

```java
// Pattern: methodName_testScenario_expectedBehavior
@Test
void createBook_ValidInput_ReturnsCreatedBook() { }

@Test
void createBook_DuplicateIsbn_ThrowsDuplicateIsbnException() { }

@Test
void getBookById_NonExistentId_ThrowsBookNotFoundException() { }
```

### 2. AAA Pattern

```java
@Test
void updateStock_ValidQuantity_UpdatesSuccessfully() {
    // Arrange
    Book book = createTestBook();
    when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
    when(bookRepository.save(book)).thenReturn(book);

    // Act
    BookDto result = bookService.updateStock(1L, 25);

    // Assert
    assertEquals(75, result.getStockQuantity()); // 50 + 25
    verify(bookRepository).save(book);
}
```

### 3. Test Data Builders

```java
public class BookTestDataBuilder {
    private String title = "Default Title";
    private String author = "Default Author";
    private String isbn = "9781234567890";
    private BigDecimal price = new BigDecimal("19.99");
    private Integer stockQuantity = 50;
    private String category = "Fiction";

    public BookTestDataBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public BookTestDataBuilder withAuthor(String author) {
        this.author = author;
        return this;
    }

    public BookTestDataBuilder withLowStock(Integer quantity) {
        this.stockQuantity = quantity;
        return this;
    }

    public Book build() {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        book.setCategory(category);
        return book;
    }
}

// Usage
@Test
void findBooksWithLowStock_ReturnsLowStockBooks() {
    Book lowStockBook = new BookTestDataBuilder()
        .withTitle("Low Stock Book")
        .withLowStock(5)
        .build();
}
```

### 4. Parameterized Tests

```java
@ParameterizedTest
@ValueSource(strings = {"", " ", "invalid-isbn", "123"})
void createBook_InvalidIsbn_ThrowsValidationException(String invalidIsbn) {
    CreateBookRequestDto request = new CreateBookRequestDto();
    request.setIsbn(invalidIsbn);
    // ... set other fields

    assertThrows(ValidationException.class, () -> {
        bookService.createBook(request);
    });
}

@ParameterizedTest
@CsvSource({
    "10, 5, true",
    "5, 10, false",
    "0, 1, false"
})
void hasEnoughStock_VariousQuantities_ReturnsExpectedResult(
    int currentStock, int requestedQuantity, boolean expected) {
    
    Book book = new BookTestDataBuilder()
        .withStockQuantity(currentStock)
        .build();

    boolean result = book.hasEnoughStock(requestedQuantity);
    assertEquals(expected, result);
}
```

### 5. Testing Async Operations

```java
@Test
void processLowStockBooks_AsyncProcessing_CompletesSuccessfully() {
    // Arrange
    List<Book> lowStockBooks = Arrays.asList(
        new BookTestDataBuilder().withLowStock(2).build(),
        new BookTestDataBuilder().withLowStock(3).build()
    );
    when(bookRepository.findBooksWithLowStock(5)).thenReturn(lowStockBooks);

    // Act
    CompletableFuture<Void> future = batchProcessingService.processLowStockBooks();

    // Assert
    assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
    verify(bookRepository).findBooksWithLowStock(5);
}
```

## Code Coverage

### Coverage Goals
- **Line Coverage**: > 80%
- **Branch Coverage**: > 70%
- **Method Coverage**: > 85%

### Exclusions

```java
// Exclude from coverage
@ExcludeFromCodeCoverage
public class ConfigurationClass {
    // Configuration classes often don't need testing
}

// Exclude specific methods
@ExcludeFromCodeCoverage
public String toString() {
    return "Not tested";
}
```

### Coverage Reports

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

## Performance Testing

### Load Testing with JMeter

```xml
<!-- JMeter Test Plan for Book API -->
<TestPlan>
    <ThreadGroup numThreads="100" rampUp="10" loops="10">
        <HTTPSamplerProxy>
            <stringProp name="HTTPSampler.domain">localhost</stringProp>
            <stringProp name="HTTPSampler.port">8081</stringProp>
            <stringProp name="HTTPSampler.path">/api/v1/books</stringProp>
            <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
    </ThreadGroup>
</TestPlan>
```

### Performance Tests

```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void getAllBooks_LargeDataset_CompletesWithinTimeout() {
    // Create 1000+ books
    List<Book> books = IntStream.range(1, 1001)
        .mapToObj(i -> new BookTestDataBuilder()
            .withTitle("Book " + i)
            .withIsbn("978123456" + String.format("%04d", i))
            .build())
        .collect(Collectors.toList());
    
    when(bookRepository.findByActiveTrue()).thenReturn(books);

    // Should complete within 5 seconds
    List<BookDto> result = bookService.getAllBooks();
    assertEquals(1000, result.size());
}
```

## Interview Questions

### Unit Testing Questions

**Q: What's the difference between mocking and stubbing?**

A: 
- **Mocking**: Creates a fake object that records interactions and allows verification of method calls
- **Stubbing**: Provides predetermined responses to method calls without verification

```java
// Mocking - verifies interaction
@Mock
private BookRepository mockRepository;

@Test
void test() {
    bookService.createBook(request);
    verify(mockRepository).save(any(Book.class)); // Verification
}

// Stubbing - just returns value
when(mockRepository.findById(1L)).thenReturn(Optional.of(book));
```

**Q: How do you test exception scenarios?**

A: Use `assertThrows()` to verify exceptions are thrown:

```java
@Test
void createBook_DuplicateIsbn_ThrowsException() {
    when(bookRepository.existsByIsbn("123")).thenReturn(true);
    
    assertThrows(DuplicateIsbnException.class, () -> {
        bookService.createBook(request);
    });
}
```

### Integration Testing Questions

**Q: What are the benefits of TestContainers?**

A: 
- Real database behavior vs. H2 differences
- Isolation between test runs
- Same environment as production
- Easy cleanup
- Multiple services testing

**Q: How do you test transactional behavior?**

A: Use `@Transactional` with proper propagation:

```java
@Test
@Transactional
@Rollback // Default behavior
void testTransactionalMethod() {
    // Test will rollback automatically
}

@Test
@Transactional
@Commit // Explicitly commit
void testCommitBehavior() {
    // Changes will be committed
}
```

### Contract Testing Questions

**Q: What problems does contract testing solve?**

A: 
- Integration issues between services
- Breaking changes in APIs
- Documentation synchronization
- Consumer-driven contracts
- Faster feedback than E2E tests

**Q: Producer vs Consumer driven contracts?**

A: 
- **Producer-driven**: Provider defines contract
- **Consumer-driven**: Consumer defines what they need
- **Spring Cloud Contract**: Supports both approaches

### SonarQube Questions

**Q: What metrics does SonarQube track?**

A: 
- **Bugs**: Logical errors
- **Vulnerabilities**: Security issues  
- **Code Smells**: Maintainability issues
- **Coverage**: Test coverage percentage
- **Duplication**: Code duplication percentage

**Q: What is a Quality Gate?**

A: Set of conditions that must be met for code to be considered "ready":
- Coverage > 80%
- New bugs = 0
- Security vulnerabilities = 0
- Maintainability rating = A

This comprehensive testing documentation covers all the key aspects needed for a Spring Boot microservices interview, with practical examples and real-world scenarios.
