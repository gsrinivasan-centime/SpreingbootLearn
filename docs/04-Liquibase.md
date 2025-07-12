# Liquibase Migration

## What is Liquibase?

Liquibase is a database version control and migration tool that allows you to:
- Track database changes in version control
- Apply changes consistently across environments
- Rollback changes when needed
- Support multiple database types
- Integrate with CI/CD pipelines

## Liquibase Architecture

```
┌─────────────────────────────────────┐
│         Application Startup         │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│        Liquibase Runner             │
│    (Reads changelog files)          │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│      Database Lock Table           │
│   (Prevents concurrent migrations)  │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│    DATABASECHANGELOG Table          │
│   (Tracks applied changesets)       │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│      Target Database               │
│    (Applies migrations)             │
└─────────────────────────────────────┘
```

## Setup and Configuration

### 1. Maven Dependencies
```xml
<!-- pom.xml -->
<dependencies>
    <!-- Liquibase Core -->
    <dependency>
        <groupId>org.liquibase</groupId>
        <artifactId>liquibase-core</artifactId>
    </dependency>
    
    <!-- Liquibase Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MySQL Driver -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Liquibase Maven Plugin -->
        <plugin>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-maven-plugin</artifactId>
            <version>4.24.0</version>
            <configuration>
                <propertyFile>src/main/resources/liquibase.properties</propertyFile>
                <changeLogFile>src/main/resources/db/changelog/db.changelog-master.xml</changeLogFile>
                <diffChangeLogFile>src/main/resources/db/changelog/db.changelog-${maven.build.timestamp}.xml</diffChangeLogFile>
                <outputChangeLogFile>src/main/resources/db/changelog/db.changelog-output.xml</outputChangeLogFile>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>mysql</groupId>
                    <artifactId>mysql-connector-java</artifactId>
                    <version>8.0.33</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

### 2. Application Configuration
```yaml
# application.yml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
    drop-first: false
    contexts: ${LIQUIBASE_CONTEXTS:dev}
    default-schema: ${DB_NAME:bookstore_books}
    
  jpa:
    hibernate:
      ddl-auto: validate # Important: Let Liquibase manage schema
    show-sql: false
    
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore_books?useSSL=false&serverTimezone=UTC
    username: ${DB_USERNAME:book_user}
    password: ${DB_PASSWORD:book_password}
    driver-class-name: com.mysql.cj.jdbc.Driver

# Profile-specific configurations
---
spring:
  config:
    activate:
      on-profile: test
  liquibase:
    contexts: test
    drop-first: true

---
spring:
  config:
    activate:
      on-profile: prod
  liquibase:
    contexts: prod
    enabled: true
```

### 3. Liquibase Properties File
```properties
# src/main/resources/liquibase.properties
changeLogFile=src/main/resources/db/changelog/db.changelog-master.xml
url=jdbc:mysql://localhost:3306/bookstore_books
username=book_user
password=book_password
driver=com.mysql.cj.jdbc.Driver
outputFile=target/liquibase-output.txt
logLevel=INFO
```

## Changelog Structure

### 1. Master Changelog File
```xml
<!-- src/main/resources/db/changelog/db.changelog-master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <!-- Include version-specific changelogs -->
    <include file="db/changelog/v1.0/db.changelog-v1.0.xml"/>
    <include file="db/changelog/v1.1/db.changelog-v1.1.xml"/>
    <include file="db/changelog/v1.2/db.changelog-v1.2.xml"/>
    
    <!-- Include data migrations -->
    <include file="db/changelog/data/db.changelog-data.xml"/>
    
    <!-- Include indexes and constraints -->
    <include file="db/changelog/indexes/db.changelog-indexes.xml"/>
    
</databaseChangeLog>
```

### 2. Version 1.0 - Initial Schema
```xml
<!-- src/main/resources/db/changelog/v1.0/db.changelog-v1.0.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <!-- Create Categories Table -->
    <changeSet id="1" author="srinivasan" context="dev,prod">
        <createTable tableName="categories">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(100)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"/>
        </createTable>
        
        <rollback>
            <dropTable tableName="categories"/>
        </rollback>
    </changeSet>

    <!-- Create Books Table -->
    <changeSet id="2" author="srinivasan" context="dev,prod">
        <createTable tableName="books">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="title" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="author" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="isbn" type="VARCHAR(13)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="price" type="DECIMAL(10,2)">
                <constraints nullable="false"/>
            </column>
            <column name="stock" type="INT" defaultValue="0">
                <constraints nullable="false"/>
            </column>
            <column name="category" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="TEXT"/>
            <column name="cover_image_url" type="VARCHAR(500)"/>
            <column name="available" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"/>
            <column name="version" type="INT" defaultValue="0"/>
        </createTable>
        
        <rollback>
            <dropTable tableName="books"/>
        </rollback>
    </changeSet>

    <!-- Create Book Reviews Table -->
    <changeSet id="3" author="srinivasan" context="dev,prod">
        <createTable tableName="book_reviews">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="book_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="rating" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="review_text" type="TEXT"/>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"/>
        </createTable>
        
        <!-- Add Foreign Key Constraint -->
        <addForeignKeyConstraint
            baseTableName="book_reviews"
            baseColumnNames="book_id"
            referencedTableName="books"
            referencedColumnNames="id"
            constraintName="fk_book_reviews_book_id"
            onDelete="CASCADE"/>
            
        <rollback>
            <dropForeignKeyConstraint baseTableName="book_reviews" constraintName="fk_book_reviews_book_id"/>
            <dropTable tableName="book_reviews"/>
        </rollback>
    </changeSet>

    <!-- Add Check Constraints -->
    <changeSet id="4" author="srinivasan" context="dev,prod">
        <sql>
            ALTER TABLE books ADD CONSTRAINT chk_price CHECK (price > 0);
            ALTER TABLE books ADD CONSTRAINT chk_stock CHECK (stock >= 0);
            ALTER TABLE book_reviews ADD CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5);
        </sql>
        
        <rollback>
            <sql>
                ALTER TABLE book_reviews DROP CONSTRAINT chk_rating;
                ALTER TABLE books DROP CONSTRAINT chk_stock;
                ALTER TABLE books DROP CONSTRAINT chk_price;
            </sql>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### 3. Version 1.1 - Add Inventory Tracking
```xml
<!-- src/main/resources/db/changelog/v1.1/db.changelog-v1.1.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <!-- Add Inventory Transactions Table -->
    <changeSet id="5" author="srinivasan" context="dev,prod">
        <createTable tableName="inventory_transactions">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="book_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="transaction_type" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="quantity" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="reference_id" type="VARCHAR(100)"/>
            <column name="notes" type="TEXT"/>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="created_by" type="VARCHAR(100)"/>
        </createTable>
        
        <addForeignKeyConstraint
            baseTableName="inventory_transactions"
            baseColumnNames="book_id"
            referencedTableName="books"
            referencedColumnNames="id"
            constraintName="fk_inventory_transactions_book_id"
            onDelete="CASCADE"/>
            
        <rollback>
            <dropForeignKeyConstraint baseTableName="inventory_transactions" constraintName="fk_inventory_transactions_book_id"/>
            <dropTable tableName="inventory_transactions"/>
        </rollback>
    </changeSet>

    <!-- Add Publisher Column to Books -->
    <changeSet id="6" author="srinivasan" context="dev,prod">
        <addColumn tableName="books">
            <column name="publisher" type="VARCHAR(255)"/>
            <column name="publication_date" type="DATE"/>
            <column name="page_count" type="INT"/>
        </addColumn>
        
        <rollback>
            <dropColumn tableName="books">
                <column name="publisher"/>
                <column name="publication_date"/>
                <column name="page_count"/>
            </dropColumn>
        </rollback>
    </changeSet>

    <!-- Modify Category Column to ENUM -->
    <changeSet id="7" author="srinivasan" context="dev,prod">
        <modifyDataType tableName="books" columnName="category" newDataType="ENUM('FICTION', 'NON_FICTION', 'SCIENCE', 'TECHNOLOGY', 'BIOGRAPHY', 'MYSTERY', 'ROMANCE')"/>
        
        <rollback>
            <modifyDataType tableName="books" columnName="category" newDataType="VARCHAR(50)"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### 4. Data Migration Changelog
```xml
<!-- src/main/resources/db/changelog/data/db.changelog-data.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <!-- Insert Sample Categories -->
    <changeSet id="data-1" author="srinivasan" context="dev">
        <insert tableName="categories">
            <column name="name" value="Fiction"/>
            <column name="description" value="Fictional literature and novels"/>
        </insert>
        <insert tableName="categories">
            <column name="name" value="Technology"/>
            <column name="description" value="Technical and programming books"/>
        </insert>
        <insert tableName="categories">
            <column name="name" value="Science"/>
            <column name="description" value="Scientific research and discoveries"/>
        </insert>
        
        <rollback>
            <delete tableName="categories">
                <where>name IN ('Fiction', 'Technology', 'Science')</where>
            </delete>
        </rollback>
    </changeSet>

    <!-- Insert Sample Books -->
    <changeSet id="data-2" author="srinivasan" context="dev">
        <loadData file="db/data/sample-books.csv" tableName="books">
            <column name="id" type="NUMERIC"/>
            <column name="title" type="STRING"/>
            <column name="author" type="STRING"/>
            <column name="isbn" type="STRING"/>
            <column name="price" type="NUMERIC"/>
            <column name="stock" type="NUMERIC"/>
            <column name="category" type="STRING"/>
            <column name="description" type="STRING"/>
            <column name="available" type="BOOLEAN"/>
        </loadData>
        
        <rollback>
            <delete tableName="books">
                <where>id IN (1, 2, 3, 4, 5)</where>
            </delete>
        </rollback>
    </changeSet>

    <!-- Update Book Prices (Example of data migration) -->
    <changeSet id="data-3" author="srinivasan" context="dev,prod">
        <update tableName="books">
            <column name="price" valueNumeric="price * 1.05"/>
            <where>category = 'TECHNOLOGY'</where>
        </update>
        
        <rollback>
            <update tableName="books">
                <column name="price" valueComputed="price / 1.05"/>
                <where>category = 'TECHNOLOGY'</where>
            </update>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### 5. Sample Data CSV File
```csv
# src/main/resources/db/data/sample-books.csv
id,title,author,isbn,price,stock,category,description,available
1,"Clean Code","Robert C. Martin","9780132350884",299.99,50,"TECHNOLOGY","A Handbook of Agile Software Craftsmanship",true
2,"The Great Gatsby","F. Scott Fitzgerald","9780743273565",149.99,30,"FICTION","A classic American novel",true
3,"Sapiens","Yuval Noah Harari","9780062316097",399.99,25,"SCIENCE","A Brief History of Humankind",true
4,"Spring Boot in Action","Craig Walls","9781617292545",449.99,40,"TECHNOLOGY","Spring Boot guide for developers",true
5,"To Kill a Mockingbird","Harper Lee","9780061120084",199.99,35,"FICTION","A novel about racial injustice",true
```

### 6. Index Creation Changelog
```xml
<!-- src/main/resources/db/changelog/indexes/db.changelog-indexes.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <!-- Create Indexes for Better Performance -->
    <changeSet id="idx-1" author="srinivasan" context="dev,prod">
        <createIndex tableName="books" indexName="idx_books_category">
            <column name="category"/>
        </createIndex>
        
        <createIndex tableName="books" indexName="idx_books_author">
            <column name="author"/>
        </createIndex>
        
        <createIndex tableName="books" indexName="idx_books_available">
            <column name="available"/>
        </createIndex>
        
        <createIndex tableName="books" indexName="idx_books_created_at">
            <column name="created_at"/>
        </createIndex>
        
        <rollback>
            <dropIndex tableName="books" indexName="idx_books_category"/>
            <dropIndex tableName="books" indexName="idx_books_author"/>
            <dropIndex tableName="books" indexName="idx_books_available"/>
            <dropIndex tableName="books" indexName="idx_books_created_at"/>
        </rollback>
    </changeSet>

    <!-- Composite Indexes -->
    <changeSet id="idx-2" author="srinivasan" context="dev,prod">
        <createIndex tableName="books" indexName="idx_books_category_available_price">
            <column name="category"/>
            <column name="available"/>
            <column name="price"/>
        </createIndex>
        
        <createIndex tableName="book_reviews" indexName="idx_reviews_book_user">
            <column name="book_id"/>
            <column name="user_id"/>
        </createIndex>
        
        <rollback>
            <dropIndex tableName="books" indexName="idx_books_category_available_price"/>
            <dropIndex tableName="book_reviews" indexName="idx_reviews_book_user"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

## Advanced Liquibase Features

### 1. Conditional Changesets
```xml
<changeSet id="8" author="srinivasan" context="dev,prod">
    <preConditions onFail="MARK_RAN">
        <not>
            <columnExists tableName="books" columnName="rating_average"/>
        </not>
    </preConditions>
    
    <addColumn tableName="books">
        <column name="rating_average" type="DECIMAL(3,2)" defaultValue="0.00"/>
        <column name="review_count" type="INT" defaultValue="0"/>
    </addColumn>
</changeSet>
```

### 2. Custom SQL Changesets
```xml
<changeSet id="9" author="srinivasan" context="dev,prod">
    <sql>
        CREATE TRIGGER update_book_rating 
        AFTER INSERT ON book_reviews 
        FOR EACH ROW 
        BEGIN
            UPDATE books 
            SET rating_average = (
                SELECT AVG(rating) 
                FROM book_reviews 
                WHERE book_id = NEW.book_id
            ),
            review_count = (
                SELECT COUNT(*) 
                FROM book_reviews 
                WHERE book_id = NEW.book_id
            )
            WHERE id = NEW.book_id;
        END;
    </sql>
    
    <rollback>
        <sql>DROP TRIGGER IF EXISTS update_book_rating;</sql>
    </rollback>
</changeSet>
```

### 3. Database Functions and Procedures
```xml
<changeSet id="10" author="srinivasan" context="dev,prod">
    <sql>
        DELIMITER $$
        CREATE FUNCTION calculate_discount_price(original_price DECIMAL(10,2), discount_percent INT)
        RETURNS DECIMAL(10,2)
        READS SQL DATA
        DETERMINISTIC
        BEGIN
            RETURN original_price * (1 - discount_percent / 100.0);
        END$$
        DELIMITER ;
    </sql>
    
    <rollback>
        <sql>DROP FUNCTION IF EXISTS calculate_discount_price;</sql>
    </rollback>
</changeSet>
```

## Environment-Specific Migrations

### 1. Production-Only Changes
```xml
<changeSet id="prod-1" author="srinivasan" context="prod">
    <createTable tableName="audit_log">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="table_name" type="VARCHAR(100)"/>
        <column name="operation" type="VARCHAR(10)"/>
        <column name="old_values" type="JSON"/>
        <column name="new_values" type="JSON"/>
        <column name="user_id" type="VARCHAR(100)"/>
        <column name="timestamp" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
    </createTable>
</changeSet>
```

### 2. Test-Only Data
```xml
<changeSet id="test-1" author="srinivasan" context="test">
    <loadData file="db/data/test-books.csv" tableName="books"/>
    <loadData file="db/data/test-users.csv" tableName="book_reviews"/>
</changeSet>
```

## Liquibase Commands

### 1. Maven Commands
```bash
# Generate changelog from existing database
mvn liquibase:generateChangeLog

# Update database to latest version
mvn liquibase:update

# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=v1.0

# Show pending changes
mvn liquibase:status

# Generate diff between databases
mvn liquibase:diff

# Validate changesets
mvn liquibase:validate

# Mark all changes as executed (without running them)
mvn liquibase:changelogSync

# Clear all checksums
mvn liquibase:clearCheckSums
```

### 2. Spring Boot Integration
```java
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
    
    @Autowired
    private SpringLiquibase liquibase;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Custom logic after Liquibase migration
        log.info("Database migration completed successfully");
        
        // Verify critical tables exist
        verifyDatabaseIntegrity();
    }
    
    private void verifyDatabaseIntegrity() {
        // Custom verification logic
    }
}
```

### 3. Programmatic Liquibase Usage
```java
@Service
public class DatabaseMigrationService {
    
    @Autowired
    private DataSource dataSource;
    
    public void runMigration(String changelogFile, String contexts) throws Exception {
        Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()));
        
        Liquibase liquibase = new Liquibase(changelogFile, new ClassLoaderResourceAccessor(), database);
        
        // Run migration with specific contexts
        liquibase.update(contexts);
        
        // Or rollback
        // liquibase.rollback(1, contexts);
    }
}
```

## Best Practices

### 1. Changeset Guidelines
```xml
<!-- Good: Specific, descriptive ID and author -->
<changeSet id="2024-01-15-add-user-phone-column" author="srinivasan">
    <addColumn tableName="users">
        <column name="phone_number" type="VARCHAR(20)"/>
    </addColumn>
</changeSet>

<!-- Good: Include rollback for every changeset -->
<changeSet id="2024-01-15-create-orders-table" author="srinivasan">
    <createTable tableName="orders">
        <!-- table definition -->
    </createTable>
    <rollback>
        <dropTable tableName="orders"/>
    </rollback>
</changeSet>

<!-- Good: Use preConditions to avoid conflicts -->
<changeSet id="2024-01-15-add-index-if-not-exists" author="srinivasan">
    <preConditions onFail="MARK_RAN">
        <not>
            <indexExists tableName="books" indexName="idx_books_category"/>
        </not>
    </preConditions>
    <createIndex tableName="books" indexName="idx_books_category">
        <column name="category"/>
    </createIndex>
</changeSet>
```

### 2. Version Control Strategy
```
db/
├── changelog/
│   ├── db.changelog-master.xml
│   ├── v1.0/
│   │   └── db.changelog-v1.0.xml
│   ├── v1.1/
│   │   └── db.changelog-v1.1.xml
│   ├── data/
│   │   ├── db.changelog-data.xml
│   │   └── sample-books.csv
│   └── indexes/
│       └── db.changelog-indexes.xml
```

### 3. CI/CD Integration
```yaml
# .github/workflows/database-migration.yml
name: Database Migration

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  migrate:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: bookstore_books_test
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=3
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Wait for MySQL
      run: |
        while ! mysqladmin ping -h"127.0.0.1" --silent; do
          sleep 1
        done
    
    - name: Run Liquibase Migration
      run: |
        mvn liquibase:update \
          -Dliquibase.url=jdbc:mysql://localhost:3306/bookstore_books_test \
          -Dliquibase.username=root \
          -Dliquibase.password=root \
          -Dliquibase.contexts=test
    
    - name: Validate Migration
      run: mvn liquibase:validate
```

## Interview Questions & Answers

### Q1: How does Liquibase track which changes have been applied?

**Answer**: Liquibase uses two tables:
- **DATABASECHANGELOG**: Tracks executed changesets with checksums
- **DATABASECHANGELOGLOCK**: Prevents concurrent migrations
Each changeset has a unique ID + author + filename combination

### Q2: What happens if you modify an already executed changeset?

**Answer**: 
- Liquibase detects checksum mismatch and fails
- **Solutions**: 
  1. Create new changeset to fix the issue
  2. Use `validCheckSum` to accept the change
  3. Clear checksums with `liquibase:clearCheckSums`

### Q3: How do you handle hotfixes in production?

**Answer**:
1. Create new changeset with higher sequence number
2. Apply to production first if urgent
3. Merge back to development branches
4. Use contexts to separate environments

### Q4: What are the advantages of Liquibase over Flyway?

**Answer**:
- **XML/YAML/JSON formats** vs SQL-only in Flyway
- **Database-agnostic** changesets
- **Advanced rollback** capabilities
- **Conditional logic** and preconditions
- **Better conflict resolution**

## Next Steps

Continue to [Redis Caching](05-Redis.md) to learn about implementing caching strategies.
