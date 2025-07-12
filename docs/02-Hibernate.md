# Hibernate & JPA

## What is Hibernate and JPA?

- **JPA (Java Persistence API)**: Java specification for ORM
- **Hibernate**: Most popular JPA implementation
- **Spring Data JPA**: Spring's abstraction over JPA

## Architecture Overview

```
┌─────────────────────────────────────┐
│         Application Layer           │
│     (Services, Controllers)         │
├─────────────────────────────────────┤
│        Spring Data JPA Layer        │
│    (Repositories, Query Methods)    │
├─────────────────────────────────────┤
│           JPA Layer                 │
│      (EntityManager, JPQL)          │
├─────────────────────────────────────┤
│         Hibernate Layer             │
│   (Session, Criteria API, HQL)      │
├─────────────────────────────────────┤
│            JDBC Layer               │
│      (Connection, Statement)        │
├─────────────────────────────────────┤
│            Database                 │
│          (MySQL)                    │
└─────────────────────────────────────┘
```

## Entity Design for Bookstore

### 1. Book Entity
```java
@Entity
@Table(name = "books", indexes = {
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_author", columnList = "author"),
    @Index(name = "idx_isbn", columnList = "isbn", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, length = 255)
    private String author;
    
    @Column(unique = true, nullable = false, length = 13)
    private String isbn;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stock;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookCategory category;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    
    @Column(nullable = false)
    private Boolean available = true;
    
    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Integer version; // Optimistic locking
    
    // Relationships
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BookReview> reviews = new HashSet<>();
    
    @ManyToMany(mappedBy = "books", fetch = FetchType.LAZY)
    private Set<Order> orders = new HashSet<>();
    
    // Constructors, getters, setters, equals, hashCode
    public Book() {}
    
    public Book(String title, String author, String isbn, BigDecimal price, 
                BookCategory category, Integer stock) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.category = category;
        this.stock = stock;
    }
    
    // Business methods
    public void reduceStock(int quantity) {
        if (this.stock < quantity) {
            throw new InsufficientStockException(
                "Insufficient stock. Available: " + this.stock + ", Requested: " + quantity);
        }
        this.stock -= quantity;
        if (this.stock == 0) {
            this.available = false;
        }
    }
    
    public void addStock(int quantity) {
        this.stock += quantity;
        if (this.stock > 0) {
            this.available = true;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return Objects.equals(isbn, book.isbn);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }
}
```

### 2. User Entity
```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_phone", columnList = "encrypted_phone")
})
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String firstName;
    
    @Column(nullable = false, length = 100)
    private String lastName;
    
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(name = "encrypted_phone", length = 255)
    @Convert(converter = PhoneNumberCryptoConverter.class)
    private String phoneNumber; // Encrypted in database
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.CUSTOMER;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Integer version;
    
    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Order> orders = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BookReview> reviews = new HashSet<>();
    
    @Embedded
    private Address address;
    
    // Constructors, getters, setters
}
```

### 3. Order Entity
```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_order_date", columnList = "order_date")
})
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String orderNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;
    
    // One-to-Many with OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderItem> orderItems = new HashSet<>();
    
    @Embedded
    private ShippingAddress shippingAddress;
    
    // Many-to-Many with Book through OrderItem
    @ManyToMany
    @JoinTable(
        name = "order_books",
        joinColumns = @JoinColumn(name = "order_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> books = new HashSet<>();
    
    // Business methods
    public void addOrderItem(Book book, Integer quantity, BigDecimal unitPrice) {
        OrderItem orderItem = new OrderItem(this, book, quantity, unitPrice);
        this.orderItems.add(orderItem);
        this.books.add(book);
        calculateTotalAmount();
    }
    
    private void calculateTotalAmount() {
        this.totalAmount = orderItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

## Repository Layer

### 1. Custom Repository Interface
```java
public interface BookRepositoryCustom {
    List<Book> findBooksWithComplexCriteria(BookSearchCriteria criteria);
    Page<Book> findBooksWithSpecification(Specification<Book> spec, Pageable pageable);
}
```

### 2. Repository Implementation
```java
@Repository
public interface BookRepository extends JpaRepository<Book, Long>, 
                                      BookRepositoryCustom, 
                                      JpaSpecificationExecutor<Book> {
    
    // Query Methods (Spring Data JPA generates implementation)
    List<Book> findByCategory(BookCategory category);
    
    List<Book> findByAuthorContainingIgnoreCase(String author);
    
    @Query("SELECT b FROM Book b WHERE b.available = true AND b.stock > 0")
    List<Book> findAvailableBooks();
    
    // Named Query with Parameters
    @Query("SELECT b FROM Book b WHERE b.category = :category AND b.price BETWEEN :minPrice AND :maxPrice")
    Page<Book> findBooksByCategoryAndPriceRange(
        @Param("category") BookCategory category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Native SQL Query
    @Query(value = "SELECT * FROM books b WHERE b.isbn = :isbn", nativeQuery = true)
    Optional<Book> findByIsbnNative(@Param("isbn") String isbn);
    
    // Modifying Query
    @Modifying
    @Query("UPDATE Book b SET b.stock = b.stock - :quantity WHERE b.id = :id AND b.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
    
    // Projection
    @Query("SELECT new com.bookstore.dto.BookSummary(b.id, b.title, b.author, b.price) FROM Book b")
    List<BookSummary> findBookSummaries();
    
    // Exists Query
    boolean existsByIsbn(String isbn);
    
    // Count Query
    @Query("SELECT COUNT(b) FROM Book b WHERE b.category = :category AND b.available = true")
    long countAvailableBooksByCategory(@Param("category") BookCategory category);
}
```

### 3. Custom Repository Implementation
```java
@Repository
public class BookRepositoryImpl implements BookRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<Book> findBooksWithComplexCriteria(BookSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> book = query.from(Book.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getTitle() != null) {
            predicates.add(cb.like(cb.lower(book.get("title")), 
                "%" + criteria.getTitle().toLowerCase() + "%"));
        }
        
        if (criteria.getCategory() != null) {
            predicates.add(cb.equal(book.get("category"), criteria.getCategory()));
        }
        
        if (criteria.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(book.get("price"), criteria.getMinPrice()));
        }
        
        if (criteria.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(book.get("price"), criteria.getMaxPrice()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(book.get("createdAt")));
        
        return entityManager.createQuery(query).getResultList();
    }
    
    @Override
    public Page<Book> findBooksWithSpecification(Specification<Book> spec, Pageable pageable) {
        return findAll(spec, pageable);
    }
}
```

## Advanced JPA Features

### 1. Specifications
```java
public class BookSpecifications {
    
    public static Specification<Book> hasCategory(BookCategory category) {
        return (root, query, criteriaBuilder) -> 
            category == null ? null : criteriaBuilder.equal(root.get("category"), category);
    }
    
    public static Specification<Book> hasTitleContaining(String title) {
        return (root, query, criteriaBuilder) -> 
            title == null ? null : criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")), 
                "%" + title.toLowerCase() + "%"
            );
    }
    
    public static Specification<Book> isPriceInRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice == null) return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
            if (maxPrice == null) return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
            return criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
        };
    }
    
    public static Specification<Book> isAvailable() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("available"), true),
                criteriaBuilder.greaterThan(root.get("stock"), 0)
            );
    }
}
```

### 2. Custom Converters
```java
@Converter
public class PhoneNumberCryptoConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String phoneNumber) {
        if (phoneNumber == null) return null;
        return encryptionService.encrypt(phoneNumber);
    }
    
    @Override
    public String convertToEntityAttribute(String encryptedPhone) {
        if (encryptedPhone == null) return null;
        return encryptionService.decrypt(encryptedPhone);
    }
}
```

### 3. Entity Listeners & Auditing
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Version
    private Integer version;
}

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}
```

### 4. Transaction Management
```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Order createOrder(CreateOrderRequest request) {
        // This method runs in a transaction
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(getCurrentUser());
        
        for (OrderItemRequest item : request.getItems()) {
            Book book = bookRepository.findById(item.getBookId())
                .orElseThrow(() -> new BookNotFoundException("Book not found"));
            
            // Optimistic locking will prevent concurrent modifications
            book.reduceStock(item.getQuantity());
            bookRepository.save(book);
            
            order.addOrderItem(book, item.getQuantity(), book.getPrice());
        }
        
        Order savedOrder = orderRepository.save(order);
        
        // Send event after successful transaction
        publishOrderCreatedEvent(savedOrder);
        
        return savedOrder;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        // This runs in a separate transaction
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        order.setStatus(newStatus);
        orderRepository.save(order);
    }
    
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        // Read-only transaction for better performance
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }
}
```

## Performance Optimization

### 1. Lazy vs Eager Loading
```java
@Entity
public class Book {
    // Lazy loading (default for collections)
    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    private Set<BookReview> reviews = new HashSet<>();
    
    // Eager loading (use sparingly)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;
}
```

### 2. N+1 Problem Solutions
```java
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Solution 1: Join Fetch
    @Query("SELECT b FROM Book b JOIN FETCH b.reviews WHERE b.id = :id")
    Optional<Book> findByIdWithReviews(@Param("id") Long id);
    
    // Solution 2: Entity Graph
    @EntityGraph(attributePaths = {"reviews", "category"})
    List<Book> findByCategory(BookCategory category);
    
    // Solution 3: Batch Fetch
    @BatchSize(size = 10)
    @OneToMany(mappedBy = "book")
    private Set<BookReview> reviews;
}
```

### 3. Second Level Cache
```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Book {
    // Entity will be cached in second-level cache
}

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT b FROM Book b WHERE b.category = :category")
    List<Book> findByCategoryCached(@Param("category") BookCategory category);
}
```

## Interview Questions & Answers

### Q1: What is the difference between JPA and Hibernate?

**Answer**: 
- **JPA**: Java specification for ORM, defines interfaces and annotations
- **Hibernate**: Implementation of JPA specification, provides additional features
- **Spring Data JPA**: Spring abstraction over JPA, provides repository pattern

### Q2: Explain the difference between @OneToMany and @ManyToOne mappings.

**Answer**:
- **@OneToMany**: One parent entity relates to many child entities (Book → Reviews)
- **@ManyToOne**: Many child entities relate to one parent entity (Reviews → Book)
- Use `mappedBy` on the "One" side to indicate the owning side
- Foreign key is maintained on the "Many" side

### Q3: What is the N+1 problem and how do you solve it?

**Answer**: 
N+1 problem occurs when:
1. One query fetches N parent entities
2. N additional queries fetch related entities for each parent

**Solutions**:
1. **JOIN FETCH**: Fetch associations in single query
2. **@EntityGraph**: Define fetch plan
3. **@BatchSize**: Batch multiple queries
4. **@Fetch(FetchMode.SUBSELECT)**: Use subquery

### Q4: What are the different cascade types in JPA?

**Answer**:
- **CascadeType.PERSIST**: Cascade persist operations
- **CascadeType.MERGE**: Cascade merge operations  
- **CascadeType.REMOVE**: Cascade remove operations
- **CascadeType.REFRESH**: Cascade refresh operations
- **CascadeType.DETACH**: Cascade detach operations
- **CascadeType.ALL**: All above operations

## Best Practices

1. **Use @Transactional appropriately** - Service layer methods
2. **Implement proper equals/hashCode** for entities
3. **Use projections** for read-only queries
4. **Optimize fetch strategies** to avoid N+1 problems
5. **Use pagination** for large result sets
6. **Implement auditing** for tracking changes
7. **Use database constraints** along with JPA validations
8. **Monitor SQL queries** in development

## Next Steps

Continue to [Database Design & Management](03-Database.md) to learn about database architecture and optimization.
