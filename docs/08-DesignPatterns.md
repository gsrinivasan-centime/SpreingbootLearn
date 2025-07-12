# Design Patterns in Spring Boot

## Overview of Design Patterns

Design patterns are reusable solutions to common problems in software design. In Spring Boot applications, several patterns are essential for building maintainable, scalable microservices.

## Core Spring Patterns

### 1. Dependency Injection (DI) Pattern

Spring's core pattern that promotes loose coupling and testability.

```java
// Poor design - tight coupling
public class BookService {
    private BookRepository repository = new BookRepository(); // Hard dependency
    
    public Book findById(Long id) {
        return repository.findById(id);
    }
}

// Good design - dependency injection
@Service
public class BookService {
    
    private final BookRepository repository;
    private final EmailService emailService;
    private final CacheService cacheService;
    
    // Constructor injection (recommended)
    public BookService(BookRepository repository, 
                      EmailService emailService,
                      CacheService cacheService) {
        this.repository = repository;
        this.emailService = emailService;
        this.cacheService = cacheService;
    }
    
    public Book findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + id));
    }
}

// Configuration class
@Configuration
public class ServiceConfig {
    
    @Bean
    @Primary
    public BookRepository bookRepository() {
        return new JpaBookRepository();
    }
    
    @Bean
    @Profile("test")
    public BookRepository testBookRepository() {
        return new InMemoryBookRepository();
    }
}
```

### 2. Singleton Pattern

Spring beans are singletons by default, ensuring single instance per container.

```java
@Service
@Scope("singleton") // Default scope
public class BookCacheService {
    
    private final Map<Long, Book> cache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("BookCacheService singleton initialized");
    }
    
    public void cacheBook(Book book) {
        cache.put(book.getId(), book);
    }
    
    public Optional<Book> getCachedBook(Long id) {
        return Optional.ofNullable(cache.get(id));
    }
}

// For stateful beans, use prototype scope
@Service
@Scope("prototype")
public class OrderProcessor {
    
    private String currentOrderId;
    private LocalDateTime startTime;
    
    public void processOrder(String orderId) {
        this.currentOrderId = orderId;
        this.startTime = LocalDateTime.now();
        // Process order
    }
}
```

## Creational Patterns

### 1. Factory Pattern

```java
// Book Factory Interface
public interface BookFactory {
    Book createBook(String type, Map<String, Object> properties);
}

// Concrete Factory Implementation
@Component
public class BookFactoryImpl implements BookFactory {
    
    @Override
    public Book createBook(String type, Map<String, Object> properties) {
        return switch (type.toUpperCase()) {
            case "EBOOK" -> createEBook(properties);
            case "PHYSICAL" -> createPhysicalBook(properties);
            case "AUDIOBOOK" -> createAudioBook(properties);
            default -> throw new IllegalArgumentException("Unknown book type: " + type);
        };
    }
    
    private EBook createEBook(Map<String, Object> properties) {
        return EBook.builder()
            .title((String) properties.get("title"))
            .author((String) properties.get("author"))
            .fileFormat((String) properties.get("format"))
            .fileSize((Long) properties.get("size"))
            .downloadUrl((String) properties.get("downloadUrl"))
            .build();
    }
    
    private PhysicalBook createPhysicalBook(Map<String, Object> properties) {
        return PhysicalBook.builder()
            .title((String) properties.get("title"))
            .author((String) properties.get("author"))
            .weight((Double) properties.get("weight"))
            .dimensions((String) properties.get("dimensions"))
            .shippingCost((BigDecimal) properties.get("shippingCost"))
            .build();
    }
    
    private AudioBook createAudioBook(Map<String, Object> properties) {
        return AudioBook.builder()
            .title((String) properties.get("title"))
            .author((String) properties.get("author"))
            .narrator((String) properties.get("narrator"))
            .duration((Duration) properties.get("duration"))
            .streamingUrl((String) properties.get("streamingUrl"))
            .build();
    }
}

// Usage in Service
@Service
public class BookCreationService {
    
    private final BookFactory bookFactory;
    private final BookRepository bookRepository;
    
    public BookCreationService(BookFactory bookFactory, BookRepository bookRepository) {
        this.bookFactory = bookFactory;
        this.bookRepository = bookRepository;
    }
    
    public Book createBook(BookCreationRequest request) {
        Book book = bookFactory.createBook(request.getType(), request.getProperties());
        return bookRepository.save(book);
    }
}
```

### 2. Builder Pattern

```java
// Book Builder
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private BigDecimal price;
    private BookCategory category;
    private String description;
    private Integer stock;
    private Boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Custom builder methods
    public static class BookBuilder {
        
        public BookBuilder withDefaults() {
            this.available = true;
            this.stock = 0;
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
            return this;
        }
        
        public BookBuilder asTechnologyBook() {
            this.category = BookCategory.TECHNOLOGY;
            return this;
        }
        
        public BookBuilder asFictionBook() {
            this.category = BookCategory.FICTION;
            return this;
        }
        
        public BookBuilder withStock(Integer stock) {
            this.stock = stock;
            this.available = stock > 0;
            return this;
        }
    }
}

// Usage
@Service
public class BookService {
    
    public Book createTechnologyBook(String title, String author, String isbn, BigDecimal price) {
        return Book.builder()
            .withDefaults()
            .title(title)
            .author(author)
            .isbn(isbn)
            .price(price)
            .asTechnologyBook()
            .withStock(10)
            .build();
    }
    
    public Book createFictionBook(CreateFictionBookRequest request) {
        return Book.builder()
            .withDefaults()
            .title(request.getTitle())
            .author(request.getAuthor())
            .isbn(request.getIsbn())
            .price(request.getPrice())
            .description(request.getDescription())
            .asFictionBook()
            .withStock(request.getInitialStock())
            .build();
    }
}
```

## Structural Patterns

### 1. Adapter Pattern

```java
// Third-party payment service
public class ExternalPaymentService {
    public PaymentResult processPayment(String cardNumber, double amount, String currency) {
        // External implementation
        return new PaymentResult("success", "TXN123");
    }
}

// Our payment interface
public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
}

// Adapter to bridge the gap
@Service
public class PaymentServiceAdapter implements PaymentService {
    
    private final ExternalPaymentService externalService;
    
    public PaymentServiceAdapter(ExternalPaymentService externalService) {
        this.externalService = externalService;
    }
    
    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        // Convert our request to external format
        PaymentResult result = externalService.processPayment(
            request.getCardNumber(),
            request.getAmount().doubleValue(),
            request.getCurrency()
        );
        
        // Convert external response to our format
        return PaymentResponse.builder()
            .success("success".equals(result.getStatus()))
            .transactionId(result.getTransactionId())
            .message(result.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }
}

// Multiple payment providers with same interface
@Service
@Primary
public class StripePaymentAdapter implements PaymentService {
    // Stripe-specific implementation
}

@Service
public class PayPalPaymentAdapter implements PaymentService {
    // PayPal-specific implementation
}

// Factory to choose payment provider
@Service
public class PaymentServiceFactory {
    
    private final Map<String, PaymentService> paymentServices;
    
    public PaymentServiceFactory(List<PaymentService> services) {
        this.paymentServices = services.stream()
            .collect(Collectors.toMap(
                service -> service.getClass().getSimpleName(),
                service -> service
            ));
    }
    
    public PaymentService getPaymentService(String provider) {
        return paymentServices.get(provider + "PaymentAdapter");
    }
}
```

### 2. Decorator Pattern

```java
// Base book service interface
public interface BookService {
    Book getBook(Long id);
    Book saveBook(Book book);
    void deleteBook(Long id);
}

// Core implementation
@Service
@Primary
public class CoreBookService implements BookService {
    
    private final BookRepository repository;
    
    public CoreBookService(BookRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Book getBook(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + id));
    }
    
    @Override
    public Book saveBook(Book book) {
        return repository.save(book);
    }
    
    @Override
    public void deleteBook(Long id) {
        repository.deleteById(id);
    }
}

// Logging decorator
@Service
public class LoggingBookServiceDecorator implements BookService {
    
    private final BookService delegate;
    
    public LoggingBookServiceDecorator(@Qualifier("coreBookService") BookService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public Book getBook(Long id) {
        log.info("Fetching book with id: {}", id);
        try {
            Book book = delegate.getBook(id);
            log.info("Successfully fetched book: {}", book.getTitle());
            return book;
        } catch (Exception e) {
            log.error("Failed to fetch book with id: {}", id, e);
            throw e;
        }
    }
    
    @Override
    public Book saveBook(Book book) {
        log.info("Saving book: {}", book.getTitle());
        Book saved = delegate.saveBook(book);
        log.info("Successfully saved book with id: {}", saved.getId());
        return saved;
    }
    
    @Override
    public void deleteBook(Long id) {
        log.info("Deleting book with id: {}", id);
        delegate.deleteBook(id);
        log.info("Successfully deleted book with id: {}", id);
    }
}

// Caching decorator
@Service
public class CachingBookServiceDecorator implements BookService {
    
    private final BookService delegate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public CachingBookServiceDecorator(
            @Qualifier("loggingBookServiceDecorator") BookService delegate,
            RedisTemplate<String, Object> redisTemplate) {
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Book getBook(Long id) {
        String cacheKey = "book:" + id;
        Book cached = (Book) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            log.debug("Cache hit for book id: {}", id);
            return cached;
        }
        
        Book book = delegate.getBook(id);
        redisTemplate.opsForValue().set(cacheKey, book, Duration.ofMinutes(15));
        log.debug("Cached book id: {}", id);
        return book;
    }
    
    @Override
    public Book saveBook(Book book) {
        Book saved = delegate.saveBook(book);
        // Invalidate cache
        if (saved.getId() != null) {
            redisTemplate.delete("book:" + saved.getId());
        }
        return saved;
    }
    
    @Override
    public void deleteBook(Long id) {
        delegate.deleteBook(id);
        redisTemplate.delete("book:" + id);
    }
}
```

## Behavioral Patterns

### 1. Strategy Pattern

```java
// Pricing strategy interface
public interface PricingStrategy {
    BigDecimal calculatePrice(Book book, PricingContext context);
}

// Different pricing strategies
@Component
public class RegularPricingStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Book book, PricingContext context) {
        return book.getPrice();
    }
}

@Component
public class MemberDiscountStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Book book, PricingContext context) {
        BigDecimal basePrice = book.getPrice();
        BigDecimal discount = context.getUser().getMembershipLevel().getDiscountPercentage();
        return basePrice.multiply(BigDecimal.ONE.subtract(discount));
    }
}

@Component
public class BulkDiscountStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Book book, PricingContext context) {
        BigDecimal basePrice = book.getPrice();
        int quantity = context.getQuantity();
        
        if (quantity >= 10) {
            return basePrice.multiply(new BigDecimal("0.85")); // 15% discount
        } else if (quantity >= 5) {
            return basePrice.multiply(new BigDecimal("0.90")); // 10% discount
        }
        
        return basePrice;
    }
}

@Component
public class SeasonalDiscountStrategy implements PricingStrategy {
    
    @Override
    public BigDecimal calculatePrice(Book book, PricingContext context) {
        BigDecimal basePrice = book.getPrice();
        LocalDate now = LocalDate.now();
        
        // Holiday season discount
        if (now.getMonthValue() == 12) {
            return basePrice.multiply(new BigDecimal("0.80")); // 20% discount
        }
        
        return basePrice;
    }
}

// Strategy factory
@Service
public class PricingStrategyFactory {
    
    private final Map<String, PricingStrategy> strategies;
    
    public PricingStrategyFactory(List<PricingStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                strategy -> strategy.getClass().getSimpleName(),
                strategy -> strategy
            ));
    }
    
    public PricingStrategy getStrategy(PricingType type) {
        return switch (type) {
            case REGULAR -> strategies.get("RegularPricingStrategy");
            case MEMBER_DISCOUNT -> strategies.get("MemberDiscountStrategy");
            case BULK_DISCOUNT -> strategies.get("BulkDiscountStrategy");
            case SEASONAL -> strategies.get("SeasonalDiscountStrategy");
        };
    }
}

// Pricing service using strategy pattern
@Service
public class PricingService {
    
    private final PricingStrategyFactory strategyFactory;
    
    public PricingService(PricingStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }
    
    public BigDecimal calculateBookPrice(Book book, PricingContext context) {
        PricingType type = determinePricingType(context);
        PricingStrategy strategy = strategyFactory.getStrategy(type);
        return strategy.calculatePrice(book, context);
    }
    
    private PricingType determinePricingType(PricingContext context) {
        // Business logic to determine which pricing strategy to use
        if (context.getQuantity() >= 5) {
            return PricingType.BULK_DISCOUNT;
        } else if (context.getUser() != null && context.getUser().isMember()) {
            return PricingType.MEMBER_DISCOUNT;
        } else if (isHolidaySeason()) {
            return PricingType.SEASONAL;
        }
        return PricingType.REGULAR;
    }
    
    private boolean isHolidaySeason() {
        return LocalDate.now().getMonthValue() == 12;
    }
}
```

### 2. Observer Pattern

```java
// Event interface
public interface BookEvent {
    Long getBookId();
    LocalDateTime getTimestamp();
}

// Specific events
@Data
@AllArgsConstructor
public class BookCreatedEvent implements BookEvent {
    private Long bookId;
    private String title;
    private String author;
    private LocalDateTime timestamp;
}

@Data
@AllArgsConstructor
public class BookUpdatedEvent implements BookEvent {
    private Long bookId;
    private Map<String, Object> changes;
    private LocalDateTime timestamp;
}

// Observer interface
public interface BookEventListener {
    void handleEvent(BookEvent event);
}

// Concrete observers
@Component
public class EmailNotificationListener implements BookEventListener {
    
    private final EmailService emailService;
    
    public EmailNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Override
    public void handleEvent(BookEvent event) {
        if (event instanceof BookCreatedEvent bookCreated) {
            emailService.notifySubscribersOfNewBook(bookCreated);
        }
    }
}

@Component
public class CacheInvalidationListener implements BookEventListener {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public CacheInvalidationListener(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public void handleEvent(BookEvent event) {
        String cacheKey = "book:" + event.getBookId();
        redisTemplate.delete(cacheKey);
        log.info("Invalidated cache for book: {}", event.getBookId());
    }
}

@Component
public class AnalyticsListener implements BookEventListener {
    
    private final AnalyticsService analyticsService;
    
    public AnalyticsListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    @Override
    public void handleEvent(BookEvent event) {
        analyticsService.recordBookEvent(event);
    }
}

// Event publisher
@Service
public class BookEventPublisher {
    
    private final List<BookEventListener> listeners;
    
    public BookEventPublisher(List<BookEventListener> listeners) {
        this.listeners = listeners;
    }
    
    public void publishEvent(BookEvent event) {
        listeners.forEach(listener -> {
            try {
                listener.handleEvent(event);
            } catch (Exception e) {
                log.error("Error handling book event", e);
            }
        });
    }
    
    @Async
    public void publishEventAsync(BookEvent event) {
        publishEvent(event);
    }
}

// Using Spring's application events (alternative approach)
@Service
public class BookService {
    
    private final BookRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    
    public BookService(BookRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }
    
    public Book createBook(CreateBookRequest request) {
        Book book = Book.builder()
            .title(request.getTitle())
            .author(request.getAuthor())
            // ... other fields
            .build();
            
        Book saved = repository.save(book);
        
        // Publish event using Spring's event mechanism
        eventPublisher.publishEvent(new BookCreatedEvent(
            saved.getId(),
            saved.getTitle(),
            saved.getAuthor(),
            LocalDateTime.now()
        ));
        
        return saved;
    }
}

// Spring event listeners
@Component
public class SpringBookEventListener {
    
    @EventListener
    @Async
    public void handleBookCreated(BookCreatedEvent event) {
        log.info("Book created: {}", event.getTitle());
        // Handle the event
    }
    
    @EventListener
    @Conditional(BookUpdateCondition.class)
    public void handleBookUpdated(BookUpdatedEvent event) {
        log.info("Book updated: {}", event.getBookId());
        // Handle the event
    }
}
```

### 3. Command Pattern

```java
// Command interface
public interface BookCommand {
    void execute();
    void undo();
}

// Concrete commands
public class CreateBookCommand implements BookCommand {
    
    private final BookRepository repository;
    private final Book book;
    private Long savedBookId;
    
    public CreateBookCommand(BookRepository repository, Book book) {
        this.repository = repository;
        this.book = book;
    }
    
    @Override
    public void execute() {
        Book saved = repository.save(book);
        this.savedBookId = saved.getId();
        log.info("Book created with ID: {}", savedBookId);
    }
    
    @Override
    public void undo() {
        if (savedBookId != null) {
            repository.deleteById(savedBookId);
            log.info("Book creation undone for ID: {}", savedBookId);
        }
    }
}

public class UpdateBookCommand implements BookCommand {
    
    private final BookRepository repository;
    private final Long bookId;
    private final Book updatedBook;
    private Book originalBook;
    
    public UpdateBookCommand(BookRepository repository, Long bookId, Book updatedBook) {
        this.repository = repository;
        this.bookId = bookId;
        this.updatedBook = updatedBook;
    }
    
    @Override
    public void execute() {
        originalBook = repository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + bookId));
        
        // Create a copy for undo
        originalBook = originalBook.toBuilder().build();
        
        repository.save(updatedBook);
        log.info("Book updated: {}", bookId);
    }
    
    @Override
    public void undo() {
        if (originalBook != null) {
            repository.save(originalBook);
            log.info("Book update undone: {}", bookId);
        }
    }
}

public class DeleteBookCommand implements BookCommand {
    
    private final BookRepository repository;
    private final Long bookId;
    private Book deletedBook;
    
    public DeleteBookCommand(BookRepository repository, Long bookId) {
        this.repository = repository;
        this.bookId = bookId;
    }
    
    @Override
    public void execute() {
        deletedBook = repository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + bookId));
        
        repository.deleteById(bookId);
        log.info("Book deleted: {}", bookId);
    }
    
    @Override
    public void undo() {
        if (deletedBook != null) {
            repository.save(deletedBook);
            log.info("Book deletion undone: {}", bookId);
        }
    }
}

// Command invoker
@Service
public class BookCommandExecutor {
    
    private final Stack<BookCommand> commandHistory = new Stack<>();
    private final int maxHistorySize = 100;
    
    public void execute(BookCommand command) {
        command.execute();
        
        // Add to history for undo functionality
        commandHistory.push(command);
        
        // Limit history size
        if (commandHistory.size() > maxHistorySize) {
            commandHistory.removeElementAt(0);
        }
    }
    
    public void undo() {
        if (!commandHistory.isEmpty()) {
            BookCommand lastCommand = commandHistory.pop();
            lastCommand.undo();
        } else {
            throw new IllegalStateException("No commands to undo");
        }
    }
    
    public boolean canUndo() {
        return !commandHistory.isEmpty();
    }
    
    public void clearHistory() {
        commandHistory.clear();
    }
}

// Usage in service
@Service
public class BookManagementService {
    
    private final BookRepository repository;
    private final BookCommandExecutor commandExecutor;
    
    public BookManagementService(BookRepository repository, BookCommandExecutor commandExecutor) {
        this.repository = repository;
        this.commandExecutor = commandExecutor;
    }
    
    public Book createBook(CreateBookRequest request) {
        Book book = Book.builder()
            .title(request.getTitle())
            .author(request.getAuthor())
            // ... other fields
            .build();
            
        CreateBookCommand command = new CreateBookCommand(repository, book);
        commandExecutor.execute(command);
        
        return repository.findByIsbn(book.getIsbn()).orElseThrow();
    }
    
    public void undoLastOperation() {
        commandExecutor.undo();
    }
}
```

## Circuit Breaker Pattern

```java
// Circuit breaker configuration
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker bookServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("bookService");
    }
    
    @Bean
    public Retry bookServiceRetry() {
        return Retry.ofDefaults("bookService");
    }
}

// Service with circuit breaker
@Service
public class ExternalBookDataService {
    
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    
    public ExternalBookDataService(RestTemplate restTemplate,
                                  CircuitBreaker bookServiceCircuitBreaker,
                                  Retry bookServiceRetry) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = bookServiceCircuitBreaker;
        this.retry = bookServiceRetry;
    }
    
    public Optional<BookMetadata> getBookMetadata(String isbn) {
        Supplier<BookMetadata> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, () -> fetchBookMetadata(isbn));
        
        // Add retry
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        
        try {
            return Optional.of(decoratedSupplier.get());
        } catch (Exception e) {
            log.error("Failed to fetch book metadata for ISBN: {}", isbn, e);
            return Optional.empty();
        }
    }
    
    private BookMetadata fetchBookMetadata(String isbn) {
        String url = "https://api.books.com/metadata/" + isbn;
        ResponseEntity<BookMetadata> response = restTemplate.getForEntity(url, BookMetadata.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ExternalServiceException("Failed to fetch book metadata");
        }
        
        return response.getBody();
    }
    
    // Fallback method
    public BookMetadata getBookMetadataWithFallback(String isbn) {
        return circuitBreaker.executeSupplier(() -> fetchBookMetadata(isbn))
            .recover(throwable -> {
                log.warn("Circuit breaker fallback for ISBN: {}", isbn);
                return createDefaultMetadata(isbn);
            });
    }
    
    private BookMetadata createDefaultMetadata(String isbn) {
        return BookMetadata.builder()
            .isbn(isbn)
            .title("Unknown Title")
            .author("Unknown Author")
            .description("Metadata not available")
            .build();
    }
}
```

## Repository Pattern

```java
// Generic repository interface
public interface GenericRepository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    void deleteById(ID id);
    boolean existsById(ID id);
    long count();
}

// Book-specific repository interface
public interface BookRepository extends GenericRepository<Book, Long> {
    List<Book> findByCategory(BookCategory category);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}

// JPA implementation
@Repository
public class JpaBookRepository implements BookRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Optional<Book> findById(Long id) {
        Book book = entityManager.find(Book.class, id);
        return Optional.ofNullable(book);
    }
    
    @Override
    public List<Book> findByCategory(BookCategory category) {
        TypedQuery<Book> query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.category = :category", Book.class);
        query.setParameter("category", category);
        return query.getResultList();
    }
    
    @Override
    public Optional<Book> findByIsbn(String isbn) {
        TypedQuery<Book> query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.isbn = :isbn", Book.class);
        query.setParameter("isbn", isbn);
        
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    // ... other methods
}

// In-memory implementation for testing
@Repository
@Profile("test")
public class InMemoryBookRepository implements BookRepository {
    
    private final Map<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(books.get(id));
    }
    
    @Override
    public Book save(Book book) {
        if (book.getId() == null) {
            book.setId(idGenerator.getAndIncrement());
        }
        books.put(book.getId(), book);
        return book;
    }
    
    @Override
    public List<Book> findByCategory(BookCategory category) {
        return books.values().stream()
            .filter(book -> book.getCategory() == category)
            .collect(Collectors.toList());
    }
    
    // ... other methods
}
```

## Interview Questions & Answers

### Q1: What design patterns are commonly used in Spring Boot applications?

**Answer**:
- **Dependency Injection**: Core Spring pattern for loose coupling
- **Singleton**: Default bean scope in Spring
- **Factory**: For creating beans and objects
- **Template Method**: JdbcTemplate, RestTemplate
- **Proxy**: AOP implementation
- **Observer**: Application events
- **Strategy**: Multiple implementations of same interface

### Q2: How does Spring implement the Singleton pattern differently from GoF Singleton?

**Answer**:
- **Spring Singleton**: One instance per Spring container (not JVM)
- **GoF Singleton**: One instance per ClassLoader/JVM
- **Spring advantages**: Better for testing, supports multiple instances in different contexts
- **Thread safety**: Spring manages thread safety automatically

### Q3: When would you use the Strategy pattern in a Spring Boot application?

**Answer**:
- **Payment processing**: Multiple payment gateways
- **Pricing algorithms**: Different pricing strategies
- **Authentication**: Multiple auth mechanisms
- **File storage**: Different storage providers
- **Implementation**: Use `@Autowired List<Interface>` to inject all strategies

### Q4: How do you implement the Circuit Breaker pattern in Spring Boot?

**Answer**:
1. **Resilience4j**: Add dependency and configure circuit breaker
2. **@CircuitBreaker**: Annotate methods with circuit breaker
3. **Fallback methods**: Provide alternative when circuit is open
4. **Configuration**: Set failure threshold, timeout, retry attempts
5. **Monitoring**: Use actuator to monitor circuit breaker state

## Best Practices

1. **Use dependency injection** instead of hard-coded dependencies
2. **Favor composition over inheritance** for flexibility
3. **Implement interfaces** for better testability
4. **Use factory patterns** for complex object creation
5. **Apply single responsibility principle** to each class
6. **Use strategy pattern** for algorithm variations
7. **Implement proper error handling** with circuit breakers
8. **Design for testability** with mock-friendly interfaces

## Next Steps

Continue to [Testing Strategies](09-Testing.md) to learn about comprehensive testing approaches for Spring Boot applications.
