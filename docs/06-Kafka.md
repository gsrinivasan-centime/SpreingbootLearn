# Kafka Messaging

## What is Apache Kafka?

Apache Kafka is a distributed streaming platform that provides:
- **High-throughput messaging**: Handle millions of messages per second
- **Fault tolerance**: Distributed and replicated across multiple servers
- **Scalability**: Horizontal scaling by adding more brokers
- **Durability**: Messages persisted to disk with configurable retention
- **Real-time processing**: Stream processing capabilities

## Kafka Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kafka Cluster                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Broker 1  │  │   Broker 2  │  │   Broker 3  │        │
│  │             │  │             │  │             │        │
│  │ Topic: books│  │ Topic: books│  │ Topic: books│        │
│  │ Partition 0 │  │ Partition 1 │  │ Partition 2 │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
           ▲                           │
           │                           ▼
┌─────────────────┐           ┌─────────────────┐
│   Book Service  │           │   User Service  │
│   (Producer)    │           │   (Consumer)    │
│                 │           │                 │
│ - Book Created  │           │ - Update Cache  │
│ - Book Updated  │           │ - Send Email    │
│ - Stock Changed │           │ - Analytics     │
└─────────────────┘           └─────────────────┘
```

## Kafka Setup and Configuration

### 1. Dependencies
```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- Jackson for JSON serialization -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Application Configuration
```yaml
# application.yml
spring:
  kafka:
    # Bootstrap servers
    bootstrap-servers: localhost:9092
    
    # Producer Configuration
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all # Wait for all replicas to acknowledge
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
      compression-type: snappy
      properties:
        spring.json.type.mapping: >
          bookCreated:com.bookstore.event.BookCreatedEvent,
          bookUpdated:com.bookstore.event.BookUpdatedEvent,
          bookDeleted:com.bookstore.event.BookDeletedEvent,
          orderCreated:com.bookstore.event.OrderCreatedEvent,
          stockChanged:com.bookstore.event.StockChangedEvent
    
    # Consumer Configuration
    consumer:
      group-id: ${spring.application.name:book-service}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false # Manual acknowledgment for reliability
      max-poll-records: 500
      fetch-min-size: 1024
      fetch-max-wait: 500
      properties:
        spring.json.trusted.packages: "com.bookstore.event"
        spring.json.type.mapping: >
          bookCreated:com.bookstore.event.BookCreatedEvent,
          bookUpdated:com.bookstore.event.BookUpdatedEvent,
          bookDeleted:com.bookstore.event.BookDeletedEvent,
          orderCreated:com.bookstore.event.OrderCreatedEvent,
          stockChanged:com.bookstore.event.StockChangedEvent
    
    # Topic Configuration
    topics:
      book-events: book-events
      order-events: order-events
      inventory-events: inventory-events
      user-events: user-events
      dead-letter: dead-letter-queue

---
# Profile-specific configurations
spring:
  config:
    activate:
      on-profile: dev
  kafka:
    bootstrap-servers: localhost:9092

---
spring:
  config:
    activate:
      on-profile: prod
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    properties:
      sasl:
        mechanism: PLAIN
        jaas:
          config: org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";
      ssl:
        endpoint-identification-algorithm: ""
```

### 3. Kafka Configuration Class
```java
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Idempotent producer for exactly-once semantics
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        
        // Set default topic
        template.setDefaultTopic("default-topic");
        
        // Add producer listener for monitoring
        template.setProducerListener(new ProducerListener<String, Object>() {
            @Override
            public void onSuccess(ProducerRecord<String, Object> producerRecord, 
                                RecordMetadata recordMetadata) {
                log.debug("Message sent successfully: key={}, topic={}, partition={}, offset={}", 
                    producerRecord.key(), recordMetadata.topic(), 
                    recordMetadata.partition(), recordMetadata.offset());
            }
            
            @Override
            public void onError(ProducerRecord<String, Object> producerRecord, 
                              RecordMetadata recordMetadata, Exception exception) {
                log.error("Failed to send message: key={}, topic={}", 
                    producerRecord.key(), producerRecord.topic(), exception);
            }
        });
        
        return template;
    }
    
    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // JSON configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.bookstore.event");
        props.put(JsonDeserializer.TYPE_MAPPINGS, 
            "bookCreated:com.bookstore.event.BookCreatedEvent," +
            "bookUpdated:com.bookstore.event.BookUpdatedEvent," +
            "bookDeleted:com.bookstore.event.BookDeletedEvent," +
            "orderCreated:com.bookstore.event.OrderCreatedEvent," +
            "stockChanged:com.bookstore.event.StockChangedEvent");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Concurrency settings
        factory.setConcurrency(3);
        
        // Error handling
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new FixedBackOff(1000L, 3L))); // Retry 3 times with 1 second delay
        
        // Dead letter publishing
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            deadLetterPublishingRecoverer(), new FixedBackOff(1000L, 3L)));
        
        return factory;
    }
    
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        return new DeadLetterPublishingRecoverer(kafkaTemplate(), 
            (record, exception) -> {
                // Send to dead letter topic
                return new TopicPartition("dead-letter-queue", record.partition());
            });
    }
    
    // Topic Creation
    @Bean
    public NewTopic bookEventsTopic() {
        return TopicBuilder.name("book-events")
            .partitions(3)
            .replicas(1)
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 days
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "snappy")
            .build();
    }
    
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order-events")
            .partitions(3)
            .replicas(1)
            .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 days
            .build();
    }
    
    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name("inventory-events")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("dead-letter-queue")
            .partitions(1)
            .replicas(1)
            .config(TopicConfig.RETENTION_MS_CONFIG, "7776000000") // 90 days
            .build();
    }
}
```

## Event-Driven Architecture

### 1. Event Classes
```java
// Base Event
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BookCreatedEvent.class, name = "bookCreated"),
    @JsonSubTypes.Type(value = BookUpdatedEvent.class, name = "bookUpdated"),
    @JsonSubTypes.Type(value = BookDeletedEvent.class, name = "bookDeleted"),
    @JsonSubTypes.Type(value = StockChangedEvent.class, name = "stockChanged")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    
    @JsonProperty("eventId")
    private String eventId = UUID.randomUUID().toString();
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("version")
    private String version = "1.0";
    
    @JsonProperty("correlationId")
    private String correlationId;
}

// Book Events
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BookCreatedEvent extends BaseEvent {
    
    @JsonProperty("bookId")
    private Long bookId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("author")
    private String author;
    
    @JsonProperty("isbn")
    private String isbn;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("stock")
    private Integer stock;
    
    public BookCreatedEvent(Long bookId, String title, String author, 
                           String isbn, BigDecimal price, String category, Integer stock) {
        super();
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.category = category;
        this.stock = stock;
        setSource("book-service");
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdatedEvent extends BaseEvent {
    
    @JsonProperty("bookId")
    private Long bookId;
    
    @JsonProperty("updatedFields")
    private Map<String, Object> updatedFields;
    
    @JsonProperty("previousValues")
    private Map<String, Object> previousValues;
    
    public BookUpdatedEvent(Long bookId, Map<String, Object> updatedFields, 
                           Map<String, Object> previousValues) {
        super();
        this.bookId = bookId;
        this.updatedFields = updatedFields;
        this.previousValues = previousValues;
        setSource("book-service");
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class StockChangedEvent extends BaseEvent {
    
    @JsonProperty("bookId")
    private Long bookId;
    
    @JsonProperty("previousStock")
    private Integer previousStock;
    
    @JsonProperty("newStock")
    private Integer newStock;
    
    @JsonProperty("changeType")
    private StockChangeType changeType;
    
    @JsonProperty("orderId")
    private String orderId; // Optional, for order-related stock changes
    
    public enum StockChangeType {
        INCREASE, DECREASE, ADJUSTMENT
    }
}

// Order Events
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent extends BaseEvent {
    
    @JsonProperty("orderId")
    private Long orderId;
    
    @JsonProperty("orderNumber")
    private String orderNumber;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    
    @JsonProperty("items")
    private List<OrderItemEvent> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private Long bookId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
```

### 2. Event Publishers

#### Book Service Producer
```java
@Service
@Slf4j
public class BookEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${spring.kafka.topics.book-events:book-events}")
    private String bookEventsTopic;
    
    @Value("${spring.kafka.topics.inventory-events:inventory-events}")
    private String inventoryEventsTopic;
    
    @Async
    public CompletableFuture<Void> publishBookCreated(BookCreatedEvent event) {
        return publishEvent(bookEventsTopic, event.getBookId().toString(), event)
            .thenRun(() -> log.info("Published BookCreatedEvent for book ID: {}", event.getBookId()));
    }
    
    @Async
    public CompletableFuture<Void> publishBookUpdated(BookUpdatedEvent event) {
        return publishEvent(bookEventsTopic, event.getBookId().toString(), event)
            .thenRun(() -> log.info("Published BookUpdatedEvent for book ID: {}", event.getBookId()));
    }
    
    @Async
    public CompletableFuture<Void> publishBookDeleted(BookDeletedEvent event) {
        return publishEvent(bookEventsTopic, event.getBookId().toString(), event)
            .thenRun(() -> log.info("Published BookDeletedEvent for book ID: {}", event.getBookId()));
    }
    
    @Async
    public CompletableFuture<Void> publishStockChanged(StockChangedEvent event) {
        return publishEvent(inventoryEventsTopic, event.getBookId().toString(), event)
            .thenRun(() -> log.info("Published StockChangedEvent for book ID: {}", event.getBookId()));
    }
    
    private CompletableFuture<Void> publishEvent(String topic, String key, Object event) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        kafkaTemplate.send(topic, key, event)
            .addCallback(
                result -> {
                    log.debug("Event published successfully: topic={}, key={}, offset={}", 
                        topic, key, result.getRecordMetadata().offset());
                    future.complete(null);
                },
                failure -> {
                    log.error("Failed to publish event: topic={}, key={}", topic, key, failure);
                    future.completeExceptionally(failure);
                }
            );
        
        return future;
    }
    
    // Idempotent publishing with deduplication
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void publishEventIdempotent(String topic, String key, Object event, String idempotencyKey) {
        // Check if event was already published using Redis or database
        if (isEventAlreadyPublished(idempotencyKey)) {
            log.warn("Event with idempotency key {} already published, skipping", idempotencyKey);
            return;
        }
        
        try {
            kafkaTemplate.send(topic, key, event).get(5, TimeUnit.SECONDS);
            markEventAsPublished(idempotencyKey);
            log.info("Event published with idempotency key: {}", idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to publish event with idempotency key: {}", idempotencyKey, e);
            throw new EventPublishingException("Failed to publish event", e);
        }
    }
    
    private boolean isEventAlreadyPublished(String idempotencyKey) {
        // Implementation using Redis or database to check if event was already published
        return false; // Simplified for example
    }
    
    private void markEventAsPublished(String idempotencyKey) {
        // Implementation to mark event as published
    }
}
```

#### User Service Producer
```java
@Service
@Slf4j
public class OrderEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${spring.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;
    
    @Transactional
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        publishOrderCreated(event);
    }
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        String key = event.getOrderId().toString();
        
        kafkaTemplate.send(orderEventsTopic, key, event)
            .addCallback(
                result -> log.info("Published OrderCreatedEvent for order ID: {}", event.getOrderId()),
                failure -> log.error("Failed to publish OrderCreatedEvent for order ID: {}", 
                    event.getOrderId(), failure)
            );
    }
}
```

### 3. Event Consumers

#### Book Service Consumer
```java
@Service
@Slf4j
public class BookEventConsumer {
    
    @Autowired
    private BookCacheService bookCacheService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AnalyticsService analyticsService;
    
    // Consumer with manual acknowledgment and error handling
    @KafkaListener(
        topics = "order-events",
        groupId = "book-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received OrderCreatedEvent: orderId={}, topic={}, partition={}, offset={}", 
                event.getOrderId(), topic, partition, offset);
            
            // Process stock reduction for ordered books
            processStockReduction(event);
            
            // Update analytics
            analyticsService.recordSale(event);
            
            // Manual acknowledgment
            acknowledgment.acknowledge();
            
            log.info("Successfully processed OrderCreatedEvent: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent: {}", event.getOrderId(), e);
            // Don't acknowledge - message will be retried
            throw e;
        }
    }
    
    // Consumer with retry and dead letter queue
    @KafkaListener(
        topics = "inventory-events",
        groupId = "book-service-group"
    )
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        include = {InventoryProcessingException.class}
    )
    public void handleInventoryEvents(StockChangedEvent event) {
        try {
            log.info("Processing inventory event for book: {}", event.getBookId());
            
            // Update cache
            bookCacheService.invalidateBookCache(event.getBookId());
            
            // Send notifications for low stock
            checkLowStockAlert(event);
            
        } catch (Exception e) {
            log.error("Failed to process inventory event for book: {}", event.getBookId(), e);
            throw new InventoryProcessingException("Failed to process inventory event", e);
        }
    }
    
    // Dead letter queue consumer
    @KafkaListener(
        topics = "dead-letter-queue",
        groupId = "dead-letter-processor"
    )
    public void handleDeadLetterMessages(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        
        log.error("Processing dead letter message from topic: {}, message: {}, error: {}", 
            topic, message, exceptionMessage);
        
        // Log to monitoring system
        // Send alert to operations team
        // Store in database for manual processing
    }
    
    private void processStockReduction(OrderCreatedEvent event) {
        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            // Reduce stock for each book in the order
            log.info("Reducing stock for book {} by {} units", item.getBookId(), item.getQuantity());
            // Implementation would call book service to reduce stock
        }
    }
    
    private void checkLowStockAlert(StockChangedEvent event) {
        if (event.getNewStock() < 10) { // Low stock threshold
            notificationService.sendLowStockAlert(event.getBookId(), event.getNewStock());
        }
    }
}
```

#### User Service Consumer
```java
@Service
@Slf4j
public class UserEventConsumer {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private UserCacheService userCacheService;
    
    @Autowired
    private RecommendationService recommendationService;
    
    @KafkaListener(
        topics = "book-events",
        groupId = "user-service-group"
    )
    public void handleBookEvents(
            @Payload BaseEvent event,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {
        
        log.info("Received book event: type={}, key={}", event.getClass().getSimpleName(), key);
        
        switch (event) {
            case BookCreatedEvent bookCreated -> handleBookCreated(bookCreated);
            case BookUpdatedEvent bookUpdated -> handleBookUpdated(bookUpdated);
            case BookDeletedEvent bookDeleted -> handleBookDeleted(bookDeleted);
            default -> log.warn("Unknown event type: {}", event.getClass().getSimpleName());
        }
    }
    
    private void handleBookCreated(BookCreatedEvent event) {
        // Send notification to users interested in the category
        List<Long> interestedUsers = recommendationService.getUsersInterestedInCategory(event.getCategory());
        
        for (Long userId : interestedUsers) {
            emailService.sendNewBookNotification(userId, event);
        }
        
        // Update recommendation cache
        recommendationService.updateRecommendationsForCategory(event.getCategory());
    }
    
    private void handleBookUpdated(BookUpdatedEvent event) {
        // Invalidate user recommendations if price or availability changed
        if (event.getUpdatedFields().containsKey("price") || 
            event.getUpdatedFields().containsKey("available")) {
            recommendationService.invalidateRecommendations(event.getBookId());
        }
    }
    
    private void handleBookDeleted(BookDeletedEvent event) {
        // Remove from user wishlists and recommendations
        recommendationService.removeBookFromAllRecommendations(event.getBookId());
    }
}
```

## Change Data Capture (CDC)

### 1. Database CDC with Debezium
```yaml
# docker-compose.yml addition for Debezium
version: '3.8'
services:
  # ... existing services ...
  
  # Debezium Connect
  debezium-connect:
    image: debezium/connect:latest
    container_name: bookstore-debezium
    depends_on:
      - kafka
      - mysql-books
      - mysql-users
    ports:
      - "8083:8083"
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: debezium_configs
      OFFSET_STORAGE_TOPIC: debezium_offsets
      STATUS_STORAGE_TOPIC: debezium_statuses
    networks:
      - bookstore-network
```

### 2. CDC Configuration
```json
// Book Service CDC Connector Configuration
{
  "name": "books-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",
    "database.hostname": "mysql-books",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "debezium",
    "database.server.id": "184054",
    "database.server.name": "bookstore-books",
    "database.include.list": "bookstore_books",
    "table.include.list": "bookstore_books.books,bookstore_books.inventory_transactions",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "bookstore.books.history",
    "include.schema.changes": "false",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "([^.]+)\\.([^.]+)\\.([^.]+)",
    "transforms.route.replacement": "cdc.$3"
  }
}
```

### 3. CDC Event Handler
```java
@Service
@Slf4j
public class CDCEventHandler {
    
    @Autowired
    private BookEventPublisher bookEventPublisher;
    
    @Autowired
    private BookCacheService bookCacheService;
    
    @KafkaListener(
        topics = "cdc.books",
        groupId = "cdc-processor"
    )
    public void handleBookCDC(@Payload String cdcEvent) {
        try {
            // Parse CDC event
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cdcPayload = mapper.readTree(cdcEvent);
            
            String operation = cdcPayload.get("op").asText();
            JsonNode after = cdcPayload.get("after");
            JsonNode before = cdcPayload.get("before");
            
            switch (operation) {
                case "c" -> handleBookInsert(after); // Create
                case "u" -> handleBookUpdate(before, after); // Update
                case "d" -> handleBookDelete(before); // Delete
                default -> log.warn("Unknown CDC operation: {}", operation);
            }
            
        } catch (Exception e) {
            log.error("Failed to process CDC event", e);
        }
    }
    
    private void handleBookInsert(JsonNode after) {
        Long bookId = after.get("id").asLong();
        log.info("CDC detected book insert: {}", bookId);
        
        // Invalidate cache
        bookCacheService.invalidateBookCache(bookId);
        
        // Publish event
        BookCreatedEvent event = createBookCreatedEvent(after);
        bookEventPublisher.publishBookCreated(event);
    }
    
    private void handleBookUpdate(JsonNode before, JsonNode after) {
        Long bookId = after.get("id").asLong();
        log.info("CDC detected book update: {}", bookId);
        
        // Check if stock changed
        int oldStock = before.get("stock").asInt();
        int newStock = after.get("stock").asInt();
        
        if (oldStock != newStock) {
            StockChangedEvent stockEvent = new StockChangedEvent(
                bookId, oldStock, newStock, 
                newStock > oldStock ? StockChangedEvent.StockChangeType.INCREASE : 
                                     StockChangedEvent.StockChangeType.DECREASE
            );
            bookEventPublisher.publishStockChanged(stockEvent);
        }
        
        // Invalidate cache
        bookCacheService.invalidateBookCache(bookId);
    }
    
    private void handleBookDelete(JsonNode before) {
        Long bookId = before.get("id").asLong();
        log.info("CDC detected book delete: {}", bookId);
        
        // Remove from cache
        bookCacheService.removeBookFromCache(bookId);
        
        // Publish event
        BookDeletedEvent event = new BookDeletedEvent(bookId);
        bookEventPublisher.publishBookDeleted(event);
    }
    
    private BookCreatedEvent createBookCreatedEvent(JsonNode bookData) {
        return new BookCreatedEvent(
            bookData.get("id").asLong(),
            bookData.get("title").asText(),
            bookData.get("author").asText(),
            bookData.get("isbn").asText(),
            new BigDecimal(bookData.get("price").asText()),
            bookData.get("category").asText(),
            bookData.get("stock").asInt()
        );
    }
}
```

## Event Sourcing Implementation

### 1. Event Store
```java
@Entity
@Table(name = "event_store")
public class EventStoreEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "event_data", columnDefinition = "JSON")
    private String eventData;
    
    @Column(name = "event_version", nullable = false)
    private Long eventVersion;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "correlation_id")
    private String correlationId;
    
    // Constructors, getters, setters
}

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntry, Long> {
    
    List<EventStoreEntry> findByAggregateIdOrderByEventVersionAsc(String aggregateId);
    
    List<EventStoreEntry> findByAggregateTypeAndTimestampAfterOrderByTimestampAsc(
        String aggregateType, LocalDateTime timestamp);
    
    Optional<EventStoreEntry> findTopByAggregateIdOrderByEventVersionDesc(String aggregateId);
}
```

### 2. Event Store Service
```java
@Service
@Transactional
public class EventStoreService {
    
    @Autowired
    private EventStoreRepository eventStoreRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void saveEvent(String aggregateId, String aggregateType, BaseEvent event) {
        try {
            // Get next version number
            Long nextVersion = getNextVersion(aggregateId);
            
            EventStoreEntry entry = new EventStoreEntry();
            entry.setAggregateId(aggregateId);
            entry.setAggregateType(aggregateType);
            entry.setEventType(event.getClass().getSimpleName());
            entry.setEventData(objectMapper.writeValueAsString(event));
            entry.setEventVersion(nextVersion);
            entry.setTimestamp(LocalDateTime.now());
            entry.setCorrelationId(event.getCorrelationId());
            
            eventStoreRepository.save(entry);
            
        } catch (Exception e) {
            throw new EventStoreException("Failed to save event", e);
        }
    }
    
    public List<BaseEvent> getEvents(String aggregateId) {
        List<EventStoreEntry> entries = eventStoreRepository
            .findByAggregateIdOrderByEventVersionAsc(aggregateId);
        
        return entries.stream()
            .map(this::deserializeEvent)
            .collect(Collectors.toList());
    }
    
    public List<BaseEvent> getEventsSince(String aggregateType, LocalDateTime since) {
        List<EventStoreEntry> entries = eventStoreRepository
            .findByAggregateTypeAndTimestampAfterOrderByTimestampAsc(aggregateType, since);
        
        return entries.stream()
            .map(this::deserializeEvent)
            .collect(Collectors.toList());
    }
    
    private Long getNextVersion(String aggregateId) {
        return eventStoreRepository.findTopByAggregateIdOrderByEventVersionDesc(aggregateId)
            .map(entry -> entry.getEventVersion() + 1)
            .orElse(1L);
    }
    
    private BaseEvent deserializeEvent(EventStoreEntry entry) {
        try {
            Class<?> eventClass = Class.forName("com.bookstore.event." + entry.getEventType());
            return (BaseEvent) objectMapper.readValue(entry.getEventData(), eventClass);
        } catch (Exception e) {
            throw new EventStoreException("Failed to deserialize event", e);
        }
    }
}
```

## Monitoring and Observability

### 1. Kafka Metrics
```java
@Component
public class KafkaMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong processingErrors = new AtomicLong(0);
    
    @PostConstruct
    public void initMetrics() {
        // Producer metrics
        Gauge.builder("kafka.messages.sent.total")
            .description("Total messages sent to Kafka")
            .register(meterRegistry, messagesSent, AtomicLong::get);
        
        // Consumer metrics
        Gauge.builder("kafka.messages.received.total")
            .description("Total messages received from Kafka")
            .register(meterRegistry, messagesReceived, AtomicLong::get);
        
        // Error metrics
        Gauge.builder("kafka.processing.errors.total")
            .description("Total processing errors")
            .register(meterRegistry, processingErrors, AtomicLong::get);
        
        // Lag metrics
        Timer.builder("kafka.consumer.lag")
            .description("Consumer lag in milliseconds")
            .register(meterRegistry);
    }
    
    public void recordMessageSent() {
        messagesSent.incrementAndGet();
    }
    
    public void recordMessageReceived() {
        messagesReceived.incrementAndGet();
    }
    
    public void recordProcessingError() {
        processingErrors.incrementAndGet();
    }
}
```

### 2. Health Checks
```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {
    
    @Autowired
    private KafkaAdmin kafkaAdmin;
    
    @Override
    public Health health() {
        try {
            // Check if Kafka cluster is accessible
            kafkaAdmin.describeTopics("book-events").get(5, TimeUnit.SECONDS);
            
            return Health.up()
                .withDetail("kafka", "Available")
                .withDetail("topics", "Accessible")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("kafka", "Connection failed")
                .withException(e)
                .build();
        }
    }
}
```

## Interview Questions & Answers

### Q1: What is the difference between Kafka and traditional message queues?

**Answer**:
- **Kafka**: Distributed streaming platform, messages stored on disk, high throughput, multiple consumers can read same message
- **Traditional MQ**: Point-to-point, message consumed once, lower throughput, better for request-response patterns
- **Kafka advantages**: Scalability, durability, replay capability, stream processing

### Q2: How do you ensure exactly-once delivery in Kafka?

**Answer**:
1. **Idempotent producer**: Enable `enable.idempotence=true`
2. **Transactional messaging**: Use Kafka transactions
3. **Consumer idempotency**: Implement idempotent message processing
4. **Deduplication**: Use unique message IDs and check before processing

### Q3: What is a Kafka partition and how does it affect scalability?

**Answer**:
- **Partition**: Ordered sequence of messages within a topic
- **Scalability**: More partitions = better parallelism and throughput
- **Consumer groups**: Each partition consumed by one consumer in a group
- **Ordering**: Only guaranteed within a partition, not across partitions

### Q4: How do you handle message ordering in Kafka?

**Answer**:
1. **Single partition**: Guarantees total ordering but limits scalability
2. **Partition key**: Messages with same key go to same partition
3. **Sequential processing**: Use single consumer per partition
4. **Application logic**: Handle out-of-order messages in application

## Best Practices

1. **Use appropriate partition keys** for even distribution
2. **Set proper retention policies** based on requirements
3. **Monitor consumer lag** and adjust accordingly
4. **Implement proper error handling** and dead letter queues
5. **Use schema registry** for message evolution
6. **Enable compression** for better performance
7. **Implement circuit breakers** for fault tolerance
8. **Design for idempotency** in message processing

## Next Steps

Continue to [Swagger Documentation](07-Swagger.md) to learn about API documentation and testing.
