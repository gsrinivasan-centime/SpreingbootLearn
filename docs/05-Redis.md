# Redis Caching

## What is Redis?

Redis (Remote Dictionary Server) is an in-memory data structure store that can be used as:
- **Database**: Persistent key-value store
- **Cache**: High-performance caching layer
- **Message Broker**: Pub/Sub messaging
- **Session Store**: Distributed session management

## Redis in Microservices Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   Book Service  │    │   User Service  │
│   (Port: 8081)  │    │   (Port: 8082)  │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          └──────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │     Redis Cluster     │
         │   (Port: 6379-6384)   │
         └───────────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌───▼────┐    ┌──────▼──────┐    ┌───▼────┐
│ Cache  │    │  Sessions   │    │ Pub/Sub│
│ Layer  │    │    Store    │    │   Hub  │
└────────┘    └─────────────┘    └────────┘
```

## Redis Setup and Configuration

### 1. Dependencies
```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Redis Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Redis Connection Pool -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
    
    <!-- Spring Cache Abstraction -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    
    <!-- JSON Serialization -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### 2. Application Configuration
```yaml
# application.yml
spring:
  redis:
    # Single Redis Instance
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 2000ms
    connect-timeout: 2000ms
    
    # Connection Pool Configuration
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 1000ms
    
    # Redis Cluster Configuration (Production)
    cluster:
      nodes:
        - redis-node-1:6379
        - redis-node-2:6379
        - redis-node-3:6379
        - redis-node-4:6379
        - redis-node-5:6379
        - redis-node-6:6379
      max-redirects: 3
    
    # Redis Sentinel Configuration (High Availability)
    sentinel:
      master: mymaster
      nodes:
        - sentinel-1:26379
        - sentinel-2:26379
        - sentinel-3:26379
  
  cache:
    type: redis
    redis:
      time-to-live: 60000ms
      cache-null-values: false
      key-prefix: "bookstore:"
      use-key-prefix: true

---
# Profile-specific Redis configurations
spring:
  config:
    activate:
      on-profile: dev
  redis:
    host: localhost
    port: 6379

---
spring:
  config:
    activate:
      on-profile: prod
  redis:
    cluster:
      nodes: ${REDIS_CLUSTER_NODES}
```

### 3. Redis Configuration Class
```java
@Configuration
@EnableCaching
@EnableRedisRepositories
public class RedisConfig {
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(2))
            .shutdownTimeout(Duration.ZERO)
            .build();
            
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // JSON Serialization
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        
        // String Serialization for keys
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // Set serializers
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }
    
    @Bean
    public RedisCacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(config)
            .build();
    }
    
    @Bean
    public CacheKeyGenerator cacheKeyGenerator() {
        return new CustomCacheKeyGenerator();
    }
}
```

## Caching Strategies

### 1. Application-Level Caching with @Cacheable

#### Service Layer Caching
```java
@Service
@Transactional
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Simple caching with default TTL
    @Cacheable(value = "books", key = "#id")
    public BookDTO getBookById(Long id) {
        log.info("Fetching book from database: {}", id);
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + id));
        return BookMapper.toDTO(book);
    }
    
    // Conditional caching
    @Cacheable(value = "books", key = "#isbn", condition = "#isbn.length() > 0")
    public BookDTO getBookByIsbn(String isbn) {
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));
        return BookMapper.toDTO(book);
    }
    
    // Caching with multiple cache names
    @Cacheable(value = {"books", "popular-books"}, 
               key = "#category + '_' + #page + '_' + #size")
    public Page<BookDTO> getBooksByCategory(BookCategory category, int page, int size) {
        log.info("Fetching books from database for category: {}", category);
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> books = bookRepository.findByCategory(category, pageable);
        return books.map(BookMapper::toDTO);
    }
    
    // Cache eviction on update
    @CachePut(value = "books", key = "#result.id")
    @CacheEvict(value = "popular-books", allEntries = true)
    public BookDTO updateBook(Long id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + id));
        
        // Update book properties
        book.setTitle(request.getTitle());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        
        Book savedBook = bookRepository.save(book);
        return BookMapper.toDTO(savedBook);
    }
    
    // Multiple cache evictions
    @Caching(evict = {
        @CacheEvict(value = "books", key = "#id"),
        @CacheEvict(value = "popular-books", allEntries = true),
        @CacheEvict(value = "book-summaries", allEntries = true)
    })
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
    
    // Custom cache key generation
    @Cacheable(value = "book-search", keyGenerator = "cacheKeyGenerator")
    public List<BookDTO> searchBooks(BookSearchCriteria criteria) {
        List<Book> books = bookRepository.findBooksWithComplexCriteria(criteria);
        return books.stream().map(BookMapper::toDTO).collect(Collectors.toList());
    }
}
```

#### Custom Cache Key Generator
```java
@Component
public class CustomCacheKeyGenerator implements KeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder key = new StringBuilder();
        key.append(target.getClass().getSimpleName()).append(":");
        key.append(method.getName()).append(":");
        
        for (Object param : params) {
            if (param != null) {
                key.append(param.toString()).append(":");
            }
        }
        
        return key.toString();
    }
}
```

### 2. Manual Redis Operations

#### Direct Redis Template Usage
```java
@Service
public class BookCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private static final String BOOK_CACHE_PREFIX = "book:";
    private static final String POPULAR_BOOKS_KEY = "popular:books";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    
    // String operations
    public void cacheBookCount(String category, long count) {
        String key = "book:count:" + category;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count), DEFAULT_TTL);
    }
    
    public Long getBookCount(String category) {
        String key = "book:count:" + category;
        String value = stringRedisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }
    
    // Hash operations
    public void cacheBookDetails(Long bookId, BookDTO book) {
        String key = BOOK_CACHE_PREFIX + bookId;
        
        Map<String, Object> bookMap = new HashMap<>();
        bookMap.put("id", book.getId());
        bookMap.put("title", book.getTitle());
        bookMap.put("author", book.getAuthor());
        bookMap.put("price", book.getPrice());
        bookMap.put("stock", book.getStock());
        
        redisTemplate.opsForHash().putAll(key, bookMap);
        redisTemplate.expire(key, DEFAULT_TTL);
    }
    
    public BookDTO getBookFromCache(Long bookId) {
        String key = BOOK_CACHE_PREFIX + bookId;
        Map<Object, Object> bookMap = redisTemplate.opsForHash().entries(key);
        
        if (bookMap.isEmpty()) {
            return null;
        }
        
        return BookDTO.builder()
            .id(Long.parseLong(bookMap.get("id").toString()))
            .title(bookMap.get("title").toString())
            .author(bookMap.get("author").toString())
            .price(new BigDecimal(bookMap.get("price").toString()))
            .stock(Integer.parseInt(bookMap.get("stock").toString()))
            .build();
    }
    
    // List operations - Popular books
    public void addToPopularBooks(Long bookId) {
        redisTemplate.opsForList().leftPush(POPULAR_BOOKS_KEY, bookId);
        redisTemplate.opsForList().trim(POPULAR_BOOKS_KEY, 0, 99); // Keep top 100
        redisTemplate.expire(POPULAR_BOOKS_KEY, Duration.ofHours(1));
    }
    
    public List<Long> getPopularBooks(int count) {
        List<Object> bookIds = redisTemplate.opsForList().range(POPULAR_BOOKS_KEY, 0, count - 1);
        return bookIds.stream()
            .map(id -> Long.parseLong(id.toString()))
            .collect(Collectors.toList());
    }
    
    // Set operations - User viewed books
    public void addViewedBook(Long userId, Long bookId) {
        String key = "user:viewed:" + userId;
        redisTemplate.opsForSet().add(key, bookId);
        redisTemplate.expire(key, Duration.ofDays(30));
    }
    
    public Set<Long> getUserViewedBooks(Long userId) {
        String key = "user:viewed:" + userId;
        Set<Object> viewedBooks = redisTemplate.opsForSet().members(key);
        return viewedBooks.stream()
            .map(id -> Long.parseLong(id.toString()))
            .collect(Collectors.toSet());
    }
    
    // Sorted Set operations - Book ratings
    public void updateBookRating(Long bookId, double rating) {
        String key = "book:ratings";
        redisTemplate.opsForZSet().add(key, bookId, rating);
    }
    
    public Set<Long> getTopRatedBooks(int count) {
        String key = "book:ratings";
        Set<Object> topBooks = redisTemplate.opsForZSet()
            .reverseRange(key, 0, count - 1);
        return topBooks.stream()
            .map(id -> Long.parseLong(id.toString()))
            .collect(Collectors.toSet());
    }
}
```

### 3. Session Management

#### Redis Session Configuration
```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes
public class SessionConfig {
    
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("BOOKSTORE_SESSION");
        serializer.setCookiePath("/");
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        serializer.setHttpOnly(true);
        serializer.setSecure(true); // HTTPS only in production
        serializer.setUseSecureCookies(true);
        return serializer;
    }
    
    @Bean
    public RedisIndexedSessionRepository sessionRepository(
            RedisOperations<Object, Object> redisTemplate) {
        RedisIndexedSessionRepository repository = 
            new RedisIndexedSessionRepository(redisTemplate);
        repository.setDefaultMaxInactiveInterval(Duration.ofMinutes(30));
        repository.setRedisKeyNamespace("bookstore:session");
        return repository;
    }
}
```

#### Session Service
```java
@Service
public class UserSessionService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SESSION_PREFIX = "session:user:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    
    public void createUserSession(Long userId, UserSessionData sessionData) {
        String key = SESSION_PREFIX + userId;
        redisTemplate.opsForValue().set(key, sessionData, SESSION_TTL);
    }
    
    public UserSessionData getUserSession(Long userId) {
        String key = SESSION_PREFIX + userId;
        Object sessionData = redisTemplate.opsForValue().get(key);
        return sessionData != null ? (UserSessionData) sessionData : null;
    }
    
    public void updateUserSession(Long userId, UserSessionData sessionData) {
        String key = SESSION_PREFIX + userId;
        redisTemplate.opsForValue().set(key, sessionData, SESSION_TTL);
    }
    
    public void invalidateUserSession(Long userId) {
        String key = SESSION_PREFIX + userId;
        redisTemplate.delete(key);
    }
    
    public void extendSessionTTL(Long userId) {
        String key = SESSION_PREFIX + userId;
        redisTemplate.expire(key, SESSION_TTL);
    }
}
```

### 4. Distributed Locking

#### Redis Distributed Lock
```java
@Service
public class RedisDistributedLock {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String LOCK_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('del', KEYS[1]) " +
        "else return 0 end";
    
    public boolean acquireLock(String lockKey, String lockValue, Duration expiration) {
        String key = LOCK_PREFIX + lockKey;
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, lockValue, expiration);
        return Boolean.TRUE.equals(success);
    }
    
    public boolean releaseLock(String lockKey, String lockValue) {
        String key = LOCK_PREFIX + lockKey;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(key), lockValue);
        return Long.valueOf(1).equals(result);
    }
    
    public <T> T executeWithLock(String lockKey, Duration expiration, 
                                 Supplier<T> action) throws Exception {
        String lockValue = UUID.randomUUID().toString();
        
        if (acquireLock(lockKey, lockValue, expiration)) {
            try {
                return action.get();
            } finally {
                releaseLock(lockKey, lockValue);
            }
        } else {
            throw new LockAcquisitionException("Failed to acquire lock: " + lockKey);
        }
    }
}

// Usage in service
@Service
public class InventoryService {
    
    @Autowired
    private RedisDistributedLock distributedLock;
    
    @Autowired
    private BookRepository bookRepository;
    
    public void updateBookStock(Long bookId, int quantity) throws Exception {
        String lockKey = "book:stock:" + bookId;
        
        distributedLock.executeWithLock(lockKey, Duration.ofSeconds(10), () -> {
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found"));
            
            book.setStock(book.getStock() + quantity);
            return bookRepository.save(book);
        });
    }
}
```

### 5. Pub/Sub Messaging

#### Redis Message Publisher
```java
@Service
public class BookEventPublisher {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String BOOK_CREATED_CHANNEL = "book:created";
    private static final String BOOK_UPDATED_CHANNEL = "book:updated";
    private static final String BOOK_DELETED_CHANNEL = "book:deleted";
    
    public void publishBookCreated(BookCreatedEvent event) {
        redisTemplate.convertAndSend(BOOK_CREATED_CHANNEL, event);
        log.info("Published book created event: {}", event.getBookId());
    }
    
    public void publishBookUpdated(BookUpdatedEvent event) {
        redisTemplate.convertAndSend(BOOK_UPDATED_CHANNEL, event);
        log.info("Published book updated event: {}", event.getBookId());
    }
    
    public void publishBookDeleted(BookDeletedEvent event) {
        redisTemplate.convertAndSend(BOOK_DELETED_CHANNEL, event);
        log.info("Published book deleted event: {}", event.getBookId());
    }
}
```

#### Redis Message Subscriber
```java
@Component
public class BookEventSubscriber {
    
    @Autowired
    private BookCacheService bookCacheService;
    
    @Autowired
    private NotificationService notificationService;
    
    @RedisMessageListener(channels = "book:created")
    public void handleBookCreated(BookCreatedEvent event) {
        log.info("Received book created event: {}", event.getBookId());
        // Invalidate related caches
        bookCacheService.invalidateCategoryCache(event.getCategory());
        
        // Send notifications
        notificationService.notifyBookCreated(event);
    }
    
    @RedisMessageListener(channels = "book:updated")
    public void handleBookUpdated(BookUpdatedEvent event) {
        log.info("Received book updated event: {}", event.getBookId());
        // Remove from cache to force refresh
        bookCacheService.evictBookFromCache(event.getBookId());
    }
    
    @RedisMessageListener(channels = "book:deleted")
    public void handleBookDeleted(BookDeletedEvent event) {
        log.info("Received book deleted event: {}", event.getBookId());
        // Clean up all related cache entries
        bookCacheService.removeAllBookData(event.getBookId());
    }
}

// Configuration for message listeners
@Configuration
public class RedisMessageConfig {
    
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            BookEventSubscriber subscriber) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Add message listeners
        container.addMessageListener(
            new MessageListenerAdapter(subscriber, "handleBookCreated"),
            new ChannelTopic("book:created")
        );
        
        container.addMessageListener(
            new MessageListenerAdapter(subscriber, "handleBookUpdated"),
            new ChannelTopic("book:updated")
        );
        
        container.addMessageListener(
            new MessageListenerAdapter(subscriber, "handleBookDeleted"),
            new ChannelTopic("book:deleted")
        );
        
        return container;
    }
}
```

## Advanced Redis Features

### 1. Redis Streams for Event Sourcing
```java
@Service
public class BookEventStreamService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String BOOK_EVENTS_STREAM = "book-events";
    
    public String addBookEvent(String eventType, Map<String, String> eventData) {
        eventData.put("eventType", eventType);
        eventData.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        RecordId recordId = redisTemplate.opsForStream()
            .add(BOOK_EVENTS_STREAM, eventData);
        
        return recordId.getValue();
    }
    
    public List<MapRecord<String, Object, Object>> readBookEvents(String lastId, int count) {
        return redisTemplate.opsForStream()
            .read(StreamOffset.after(lastId), StreamReadOptions.empty().count(count))
            .get(BOOK_EVENTS_STREAM);
    }
}
```

### 2. Redis Lua Scripts for Atomic Operations
```java
@Service
public class RedisLuaScriptService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Atomic increment with expiration
    private static final String INCREMENT_WITH_EXPIRE_SCRIPT = 
        "local current = redis.call('incr', KEYS[1]) " +
        "if current == 1 then " +
        "redis.call('expire', KEYS[1], ARGV[1]) " +
        "end " +
        "return current";
    
    public Long incrementWithExpire(String key, int expireSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
            INCREMENT_WITH_EXPIRE_SCRIPT, Long.class);
        
        return redisTemplate.execute(script, 
            Collections.singletonList(key), 
            String.valueOf(expireSeconds));
    }
    
    // Rate limiting script
    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local current = redis.call('incr', key) " +
        "if current == 1 then " +
        "redis.call('expire', key, window) " +
        "end " +
        "if current > limit then " +
        "return 0 " +
        "else " +
        "return 1 " +
        "end";
    
    public boolean isAllowed(String identifier, int limit, int windowSeconds) {
        String key = "rate_limit:" + identifier;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
            RATE_LIMIT_SCRIPT, Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(key), 
            String.valueOf(limit), 
            String.valueOf(windowSeconds));
        
        return Long.valueOf(1).equals(result);
    }
}
```

### 3. Cache Patterns Implementation

#### Cache-Aside Pattern
```java
@Service
public class BookServiceWithCacheAside {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String BOOK_CACHE_KEY = "book:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    
    public BookDTO getBook(Long bookId) {
        // Try to get from cache first
        String cacheKey = BOOK_CACHE_KEY + bookId;
        BookDTO cachedBook = (BookDTO) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedBook != null) {
            log.info("Cache hit for book: {}", bookId);
            return cachedBook;
        }
        
        // Cache miss - fetch from database
        log.info("Cache miss for book: {}", bookId);
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + bookId));
        
        BookDTO bookDTO = BookMapper.toDTO(book);
        
        // Store in cache
        redisTemplate.opsForValue().set(cacheKey, bookDTO, CACHE_TTL);
        
        return bookDTO;
    }
    
    public BookDTO updateBook(Long bookId, UpdateBookRequest request) {
        // Update in database
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException("Book not found: " + bookId));
        
        book.setTitle(request.getTitle());
        book.setPrice(request.getPrice());
        Book savedBook = bookRepository.save(book);
        
        BookDTO bookDTO = BookMapper.toDTO(savedBook);
        
        // Update cache
        String cacheKey = BOOK_CACHE_KEY + bookId;
        redisTemplate.opsForValue().set(cacheKey, bookDTO, CACHE_TTL);
        
        return bookDTO;
    }
    
    public void deleteBook(Long bookId) {
        // Delete from database
        bookRepository.deleteById(bookId);
        
        // Remove from cache
        String cacheKey = BOOK_CACHE_KEY + bookId;
        redisTemplate.delete(cacheKey);
    }
}
```

#### Write-Through Cache Pattern
```java
@Service
public class BookServiceWithWriteThrough {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public BookDTO createBook(CreateBookRequest request) {
        // Create in database
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPrice(request.getPrice());
        
        Book savedBook = bookRepository.save(book);
        BookDTO bookDTO = BookMapper.toDTO(savedBook);
        
        // Immediately write to cache
        String cacheKey = "book:" + savedBook.getId();
        redisTemplate.opsForValue().set(cacheKey, bookDTO, Duration.ofMinutes(15));
        
        return bookDTO;
    }
}
```

## Monitoring and Metrics

### 1. Redis Health Check
```java
@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Health health() {
        try {
            String pingResult = redisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });
            
            if ("PONG".equals(pingResult)) {
                return Health.up()
                    .withDetail("redis", "Available")
                    .withDetail("ping", pingResult)
                    .build();
            } else {
                return Health.down()
                    .withDetail("redis", "Not responding correctly")
                    .withDetail("ping", pingResult)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", "Connection failed")
                .withException(e)
                .build();
        }
    }
}
```

### 2. Cache Metrics
```java
@Component
public class CacheMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    @PostConstruct
    public void initMetrics() {
        Gauge.builder("cache.hits")
            .description("Number of cache hits")
            .register(meterRegistry, cacheHits, AtomicLong::get);
            
        Gauge.builder("cache.misses")
            .description("Number of cache misses")
            .register(meterRegistry, cacheMisses, AtomicLong::get);
            
        Gauge.builder("cache.hit.ratio")
            .description("Cache hit ratio")
            .register(meterRegistry, this, this::calculateHitRatio);
    }
    
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    private double calculateHitRatio(CacheMetrics metrics) {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
}
```

## Interview Questions & Answers

### Q1: What are the different caching strategies and when would you use each?

**Answer**:
- **Cache-Aside**: Application manages cache. Good for read-heavy loads
- **Write-Through**: Write to cache and DB together. Ensures consistency
- **Write-Behind**: Write to cache first, DB later. Better performance
- **Refresh-Ahead**: Proactively refresh before expiration

### Q2: How do you handle cache invalidation in a distributed system?

**Answer**:
1. **TTL-based expiration**: Simple but may serve stale data
2. **Event-driven invalidation**: Use pub/sub to notify cache changes
3. **Version-based**: Include version in cache keys
4. **Tag-based**: Group related cache entries with tags

### Q3: What is Redis clustering and how does it work?

**Answer**:
- **Hash slots**: 16384 slots distributed across nodes
- **Automatic sharding**: Data distributed based on key hash
- **Master-slave replication**: Each master has replicas
- **Automatic failover**: Slaves promote to masters

### Q4: How do you ensure data consistency between cache and database?

**Answer**:
1. **Cache-aside pattern** with proper error handling
2. **Write-through/write-behind** patterns
3. **Event sourcing** to track all changes
4. **Distributed transactions** (2PC) for critical operations
5. **Eventual consistency** with compensation

## Best Practices

1. **Set appropriate TTL** for all cached data
2. **Use consistent key naming** conventions
3. **Monitor cache hit ratios** and performance
4. **Implement proper error handling** for cache failures
5. **Use connection pooling** for better performance
6. **Serialize data efficiently** (JSON vs Binary)
7. **Implement circuit breakers** for cache dependencies
8. **Plan for cache warm-up** strategies

## Next Steps

Continue to [Kafka Messaging](06-Kafka.md) to learn about event-driven communication between microservices.
