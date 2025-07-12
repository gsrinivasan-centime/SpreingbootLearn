# Database Design & Management

## Database Architecture for Microservices

### 1. Database Per Service Pattern

```
┌─────────────────┐    ┌─────────────────┐
│   Book Service  │    │   User Service  │
│                 │    │                 │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          ▼                      ▼
┌─────────────────┐    ┌─────────────────┐
│ bookstore_books │    │ bookstore_users │
│   (MySQL DB)    │    │   (MySQL DB)    │
└─────────────────┘    └─────────────────┘
```

**Benefits:**
- Data isolation between services
- Independent scaling
- Technology diversity
- Fault isolation

**Challenges:**
- No ACID transactions across services
- Data consistency challenges
- Increased complexity

### 2. Database Schema Design

#### Book Service Database Schema
```sql
-- bookstore_books database schema

CREATE DATABASE bookstore_books CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookstore_books;

-- Categories table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);

-- Books table
CREATE TABLE books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(13) UNIQUE NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    category ENUM('FICTION', 'NON_FICTION', 'SCIENCE', 'TECHNOLOGY', 'BIOGRAPHY', 'MYSTERY', 'ROMANCE') NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    
    INDEX idx_category (category),
    INDEX idx_author (author),
    INDEX idx_isbn (isbn),
    INDEX idx_available (available),
    INDEX idx_created_at (created_at),
    
    CONSTRAINT chk_price CHECK (price > 0),
    CONSTRAINT chk_stock CHECK (stock >= 0)
);

-- Book reviews table
CREATE TABLE book_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL, -- Reference to user service
    rating INT NOT NULL,
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_book_id (book_id),
    INDEX idx_user_id (user_id),
    INDEX idx_rating (rating),
    
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Inventory tracking table
CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    transaction_type ENUM('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT') NOT NULL,
    quantity INT NOT NULL,
    reference_id VARCHAR(100), -- Order ID or supplier ID
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_book_id (book_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_reference_id (reference_id),
    INDEX idx_created_at (created_at)
);
```

#### User Service Database Schema
```sql
-- bookstore_users database schema

CREATE DATABASE bookstore_users CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookstore_users;

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    encrypted_phone VARCHAR(255), -- Encrypted phone number
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN', 'MODERATOR') NOT NULL DEFAULT 'CUSTOMER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    
    INDEX idx_email (email),
    INDEX idx_phone (encrypted_phone),
    INDEX idx_role (role),
    INDEX idx_active (active),
    INDEX idx_created_at (created_at)
);

-- User addresses table
CREATE TABLE user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type ENUM('HOME', 'WORK', 'BILLING', 'SHIPPING') NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_default (is_default)
);

-- Orders table
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivery_date DATE NULL,
    shipping_address_id BIGINT,
    payment_method ENUM('CARD', 'UPI', 'CASH_ON_DELIVERY') NOT NULL,
    payment_status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (shipping_address_id) REFERENCES user_addresses(id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_number (order_number),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    INDEX idx_payment_status (payment_status),
    
    CONSTRAINT chk_total_amount CHECK (total_amount > 0)
);

-- Order items table
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL, -- Reference to book service
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_book_id (book_id),
    
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_unit_price CHECK (unit_price > 0)
);

-- User sessions for Redis backup
CREATE TABLE user_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_data JSON,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);
```

### 3. Database Configuration

#### MySQL Configuration (my.cnf)
```ini
[mysqld]
# Basic Settings
server-id = 1
port = 3306
socket = /var/run/mysqld/mysqld.sock
datadir = /var/lib/mysql

# Character Set
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# InnoDB Settings
default-storage-engine = INNODB
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
innodb_file_per_table = 1

# Connection Settings
max_connections = 200
max_connect_errors = 10000
wait_timeout = 28800
interactive_timeout = 28800

# Query Cache
query_cache_type = 1
query_cache_size = 128M
query_cache_limit = 2M

# Logging
general_log = 0
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

# Binary Logging for Replication
log-bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7

# Performance Schema
performance_schema = ON
```

#### Spring Boot Database Configuration
```yaml
# application.yml
spring:
  datasource:
    # Connection Pool Configuration
    hikari:
      pool-name: BookstoreHikariPool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      
    # Book Service Database
    book-db:
      jdbc-url: jdbc:mysql://localhost:3306/bookstore_books?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      username: ${DB_USERNAME:book_user}
      password: ${DB_PASSWORD:book_password}
      driver-class-name: com.mysql.cj.jdbc.Driver
      
    # User Service Database  
    user-db:
      jdbc-url: jdbc:mysql://localhost:3306/bookstore_users?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      username: ${DB_USERNAME:user_user}
      password: ${DB_PASSWORD:user_password}
      driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate # Use Liquibase for schema management
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
    show-sql: false
    open-in-view: false # Disable OSIV to prevent lazy loading issues
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
          order_inserts: true
          order_updates: true
        connection:
          provider_disables_autocommit: true
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

### 4. Database Connection Configuration

#### Multiple DataSource Configuration
```java
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    
    // Book Service DataSource
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.book-db")
    public DataSource bookDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean bookEntityManagerFactory(
            @Qualifier("bookDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bookstore.book.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.show_sql", "false");
        em.setJpaProperties(props);
        
        return em;
    }
    
    @Bean
    @Primary
    public PlatformTransactionManager bookTransactionManager(
            @Qualifier("bookEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
    
    // User Service DataSource (if in same application)
    @Bean
    @ConfigurationProperties("spring.datasource.user-db")
    public DataSource userDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
            @Qualifier("userDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bookstore.user.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        return em;
    }
    
    @Bean
    public PlatformTransactionManager userTransactionManager(
            @Qualifier("userEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

### 5. Database Indexing Strategy

#### Index Analysis and Optimization
```sql
-- Analyze query performance
EXPLAIN FORMAT=JSON 
SELECT b.* FROM books b 
WHERE b.category = 'FICTION' 
  AND b.available = true 
  AND b.price BETWEEN 100 AND 500
ORDER BY b.created_at DESC;

-- Create composite indexes
CREATE INDEX idx_books_category_available_price 
ON books(category, available, price);

CREATE INDEX idx_books_available_created_at 
ON books(available, created_at DESC);

-- Monitor index usage
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    COLUMN_NAME
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'bookstore_books';

-- Check for unused indexes
SELECT 
    s.table_schema,
    s.table_name,
    s.index_name,
    s.cardinality,
    t.table_rows
FROM information_schema.statistics s
LEFT JOIN information_schema.tables t 
    ON s.table_schema = t.table_schema 
    AND s.table_name = t.table_name
WHERE s.table_schema = 'bookstore_books'
    AND s.index_name != 'PRIMARY';
```

### 6. Database Partitioning

#### Partitioning Large Tables
```sql
-- Partition orders table by date
ALTER TABLE orders 
PARTITION BY RANGE (YEAR(order_date)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- Partition inventory_transactions by date
ALTER TABLE inventory_transactions
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION pt2023 VALUES LESS THAN (2024),
    PARTITION pt2024 VALUES LESS THAN (2025),
    PARTITION pt2025 VALUES LESS THAN (2026),
    PARTITION pt_future VALUES LESS THAN MAXVALUE
);
```

### 7. Database Monitoring and Maintenance

#### Performance Monitoring Queries
```sql
-- Monitor slow queries
SELECT 
    query_time,
    lock_time,
    rows_sent,
    rows_examined,
    sql_text
FROM mysql.slow_log 
ORDER BY query_time DESC 
LIMIT 10;

-- Check table sizes
SELECT 
    table_name,
    table_rows,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)',
    ROUND((data_length) / 1024 / 1024, 2) AS 'Data (MB)',
    ROUND((index_length) / 1024 / 1024, 2) AS 'Index (MB)'
FROM information_schema.tables 
WHERE table_schema = 'bookstore_books'
ORDER BY (data_length + index_length) DESC;

-- Monitor connection usage
SHOW PROCESSLIST;

-- Check InnoDB status
SHOW ENGINE INNODB STATUS;
```

### 8. Backup and Recovery Strategy

#### Automated Backup Script
```bash
#!/bin/bash
# backup_databases.sh

BACKUP_DIR="/var/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
MYSQL_USER="backup_user"
MYSQL_PASSWORD="backup_password"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup Book Service Database
mysqldump -u $MYSQL_USER -p$MYSQL_PASSWORD \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    bookstore_books > $BACKUP_DIR/bookstore_books_$DATE.sql

# Backup User Service Database
mysqldump -u $MYSQL_USER -p$MYSQL_PASSWORD \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    bookstore_users > $BACKUP_DIR/bookstore_users_$DATE.sql

# Compress backups
gzip $BACKUP_DIR/bookstore_books_$DATE.sql
gzip $BACKUP_DIR/bookstore_users_$DATE.sql

# Remove backups older than 30 days
find $BACKUP_DIR -name "*.sql.gz" -type f -mtime +30 -delete

echo "Backup completed: $DATE"
```

### 9. Database Security

#### Security Best Practices
```sql
-- Create specific users for services
CREATE USER 'book_service'@'%' IDENTIFIED BY 'strong_password_123!';
CREATE USER 'user_service'@'%' IDENTIFIED BY 'strong_password_456!';

-- Grant minimal required permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON bookstore_books.* TO 'book_service'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON bookstore_users.* TO 'user_service'@'%';

-- Create read-only user for analytics
CREATE USER 'analytics_user'@'%' IDENTIFIED BY 'analytics_password_789!';
GRANT SELECT ON bookstore_books.* TO 'analytics_user'@'%';
GRANT SELECT ON bookstore_users.orders TO 'analytics_user'@'%';
GRANT SELECT ON bookstore_users.order_items TO 'analytics_user'@'%';

-- Enable SSL connections
ALTER USER 'book_service'@'%' REQUIRE SSL;
ALTER USER 'user_service'@'%' REQUIRE SSL;

FLUSH PRIVILEGES;
```

### 10. Data Encryption

#### Column-Level Encryption
```java
@Entity
public class User {
    
    @Column(name = "encrypted_phone")
    @Convert(converter = PhoneEncryptionConverter.class)
    private String phoneNumber;
    
    @Column(name = "encrypted_ssn")
    @Convert(converter = SSNEncryptionConverter.class)
    private String socialSecurityNumber;
}

@Component
public class PhoneEncryptionConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private AESEncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String plainText) {
        try {
            return plainText != null ? encryptionService.encrypt(plainText) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting phone number", e);
        }
    }
    
    @Override
    public String convertToEntityAttribute(String encryptedText) {
        try {
            return encryptedText != null ? encryptionService.decrypt(encryptedText) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting phone number", e);
        }
    }
}
```

## Interview Questions & Answers

### Q1: How do you ensure data consistency across microservices?

**Answer**: 
1. **Eventual Consistency**: Use event-driven architecture with Kafka
2. **Saga Pattern**: Manage distributed transactions
3. **Two-Phase Commit**: For critical transactions (sparingly)
4. **Compensating Actions**: Rollback operations
5. **Idempotency**: Ensure operations can be retried safely

### Q2: What are the challenges of database per service pattern?

**Answer**:
- **No ACID across services**: Can't use database transactions
- **Data duplication**: May need to replicate reference data
- **Complex queries**: Can't join across service boundaries
- **Data consistency**: Eventual consistency model
- **Increased operational complexity**: Multiple databases to manage

### Q3: How do you handle database migrations in production?

**Answer**:
1. **Use Liquibase/Flyway** for version control
2. **Blue-green deployments** to minimize downtime
3. **Backward compatible changes** first
4. **Test migrations** on production-like data
5. **Rollback strategy** for failed migrations

### Q4: What strategies do you use for database performance optimization?

**Answer**:
1. **Proper indexing** based on query patterns
2. **Connection pooling** to manage connections
3. **Query optimization** using EXPLAIN plans
4. **Caching** at application and database level
5. **Partitioning** for large tables
6. **Read replicas** for read-heavy workloads

## Best Practices

1. **Separate databases** per microservice
2. **Use connection pooling** efficiently
3. **Implement proper indexing** strategy
4. **Monitor database performance** continuously
5. **Regular backups** and disaster recovery testing
6. **Security hardening** with minimal privileges
7. **Encrypt sensitive data** at rest and in transit
8. **Use database migrations** for schema changes

## Next Steps

Continue to [Liquibase Migration](04-Liquibase.md) to learn about database version control and migrations.
