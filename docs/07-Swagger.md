# Swagger Documentation

## What is Swagger/OpenAPI?

Swagger/OpenAPI is a specification for describing REST APIs that provides:
- **Interactive documentation**: Test APIs directly from browser
- **Code generation**: Generate client SDKs and server stubs
- **API validation**: Validate requests and responses
- **Standardization**: Consistent API documentation format

## OpenAPI 3.0 Specification

```yaml
# Basic OpenAPI structure
openapi: 3.0.3
info:
  title: Bookstore API
  description: Online Bookstore Microservices API
  version: 1.0.0
  contact:
    name: API Support
    email: support@bookstore.com
servers:
  - url: http://localhost:8081
    description: Book Service - Development
  - url: http://localhost:8082
    description: User Service - Development
```

## Spring Boot Swagger Integration

### 1. Dependencies
```xml
<!-- pom.xml -->
<dependencies>
    <!-- SpringDoc OpenAPI UI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.2.0</version>
    </dependency>
    
    <!-- Bean Validation for documentation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Security for protected endpoints -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

### 2. OpenAPI Configuration
```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Book Service API",
        version = "1.0.0",
        description = "Online Bookstore - Book Management Service",
        contact = @Contact(
            name = "Development Team",
            email = "dev@bookstore.com",
            url = "https://bookstore.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8081",
            description = "Development Server"
        ),
        @Server(
            url = "https://api.bookstore.com",
            description = "Production Server"
        )
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Authentication"
)
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Book Service API")
                .version("1.0.0")
                .description("""
                    # Online Bookstore - Book Service API
                    
                    This API provides endpoints for managing books, categories, and inventory 
                    in the online bookstore application.
                    
                    ## Features
                    - CRUD operations for books
                    - Category management
                    - Inventory tracking
                    - Search and filtering
                    - Reviews and ratings
                    
                    ## Authentication
                    This API uses JWT Bearer tokens for authentication. Include the token 
                    in the Authorization header:
                    ```
                    Authorization: Bearer <your-jwt-token>
                    ```
                    """)
                .contact(new Contact()
                    .name("API Support")
                    .email("support@bookstore.com")
                    .url("https://bookstore.com/support"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addServersItem(new Server()
                .url("http://localhost:8081")
                .description("Development server"))
            .addServersItem(new Server()
                .url("https://api.bookstore.com")
                .description("Production server"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
    
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .displayName("Public API")
            .pathsToMatch("/api/public/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .displayName("Admin API")
            .pathsToMatch("/api/admin/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi booksApi() {
        return GroupedOpenApi.builder()
            .group("books")
            .displayName("Books API")
            .pathsToMatch("/api/books/**")
            .build();
    }
}
```

### 3. Application Configuration
```yaml
# application.yml
springdoc:
  api-docs:
    enabled: true
    path: /api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
    try-it-out-enabled: true
    filter: true
    display-request-duration: true
    display-operation-id: true
    default-models-expand-depth: 2
    default-model-expand-depth: 2
    
  # Group configuration
  group-configs:
    - group: books
      display-name: Books API
      paths-to-match: /api/books/**
    - group: admin
      display-name: Admin API
      paths-to-match: /api/admin/**

# Hide Swagger in production
---
spring:
  config:
    activate:
      on-profile: prod
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## API Documentation with Annotations

### 1. Controller Documentation
```java
@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "Book management operations")
@Validated
public class BookController {
    
    @Autowired
    private BookService bookService;
    
    @Operation(
        summary = "Get all books",
        description = "Retrieve a paginated list of all books with optional filtering",
        tags = {"Books"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved books",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedBooksResponse.class),
                examples = @ExampleObject(
                    name = "Successful response",
                    value = """
                        {
                          "content": [
                            {
                              "id": 1,
                              "title": "Clean Code",
                              "author": "Robert C. Martin",
                              "isbn": "9780132350884",
                              "price": 299.99,
                              "category": "TECHNOLOGY",
                              "available": true
                            }
                          ],
                          "pageable": {
                            "page": 0,
                            "size": 10,
                            "totalElements": 1,
                            "totalPages": 1
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<Page<BookDTO>> getAllBooks(
            @Parameter(
                description = "Page number (0-based)",
                example = "0"
            )
            @RequestParam(defaultValue = "0") 
            @Min(value = 0, message = "Page number must be non-negative") 
            int page,
            
            @Parameter(
                description = "Number of items per page",
                example = "10"
            )
            @RequestParam(defaultValue = "10") 
            @Min(value = 1, message = "Size must be positive")
            @Max(value = 100, message = "Size must not exceed 100") 
            int size,
            
            @Parameter(
                description = "Filter by book category",
                example = "TECHNOLOGY"
            )
            @RequestParam(required = false) 
            BookCategory category,
            
            @Parameter(
                description = "Search term for title or author",
                example = "Clean Code"
            )
            @RequestParam(required = false) 
            String search,
            
            @Parameter(
                description = "Minimum price filter",
                example = "100.00"
            )
            @RequestParam(required = false) 
            @DecimalMin(value = "0.0", message = "Minimum price must be non-negative")
            BigDecimal minPrice,
            
            @Parameter(
                description = "Maximum price filter",
                example = "500.00"
            )
            @RequestParam(required = false) 
            @DecimalMin(value = "0.0", message = "Maximum price must be non-negative")
            BigDecimal maxPrice) {
        
        BookSearchCriteria criteria = BookSearchCriteria.builder()
            .category(category)
            .search(search)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .build();
            
        Page<BookDTO> books = bookService.searchBooks(criteria, page, size);
        return ResponseEntity.ok(books);
    }
    
    @Operation(
        summary = "Get book by ID",
        description = "Retrieve a specific book by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Book found successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Book not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(
            @Parameter(
                description = "Unique identifier of the book",
                required = true,
                example = "1"
            )
            @PathVariable 
            @Positive(message = "Book ID must be positive") 
            Long id) {
        
        BookDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }
    
    @Operation(
        summary = "Create a new book",
        description = "Add a new book to the catalog"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Book created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid book data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Book with ISBN already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BookDTO> createBook(
            @Parameter(
                description = "Book data to create",
                required = true
            )
            @Valid @RequestBody CreateBookRequest request) {
        
        BookDTO createdBook = bookService.createBook(request);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(createdBook.getId())
            .toUri();
            
        return ResponseEntity.created(location).body(createdBook);
    }
    
    @Operation(
        summary = "Update book",
        description = "Update an existing book's information"
    )
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BookDTO> updateBook(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateBookRequest request) {
        
        BookDTO updatedBook = bookService.updateBook(id, request);
        return ResponseEntity.ok(updatedBook);
    }
    
    @Operation(
        summary = "Delete book",
        description = "Remove a book from the catalog"
    )
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteBook(
            @PathVariable @Positive Long id) {
        
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 2. DTO Documentation
```java
@Schema(
    description = "Book data transfer object",
    example = """
        {
          "id": 1,
          "title": "Clean Code",
          "author": "Robert C. Martin",
          "isbn": "9780132350884",
          "price": 299.99,
          "stock": 50,
          "category": "TECHNOLOGY",
          "description": "A Handbook of Agile Software Craftsmanship",
          "available": true,
          "createdAt": "2024-01-15T10:30:00",
          "updatedAt": "2024-01-15T10:30:00"
        }
        """
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDTO {
    
    @Schema(
        description = "Unique identifier of the book",
        example = "1",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;
    
    @Schema(
        description = "Title of the book",
        example = "Clean Code",
        required = true,
        maxLength = 255
    )
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Schema(
        description = "Author of the book",
        example = "Robert C. Martin",
        required = true,
        maxLength = 255
    )
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    private String author;
    
    @Schema(
        description = "ISBN number of the book",
        example = "9780132350884",
        required = true,
        pattern = "^\\d{13}$"
    )
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^\\d{13}$", message = "ISBN must be 13 digits")
    private String isbn;
    
    @Schema(
        description = "Price of the book in USD",
        example = "299.99",
        required = true,
        minimum = "0"
    )
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal price;
    
    @Schema(
        description = "Available stock quantity",
        example = "50",
        required = true,
        minimum = "0"
    )
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be non-negative")
    private Integer stock;
    
    @Schema(
        description = "Book category",
        example = "TECHNOLOGY",
        required = true,
        allowableValues = {"FICTION", "NON_FICTION", "SCIENCE", "TECHNOLOGY", "BIOGRAPHY", "MYSTERY", "ROMANCE"}
    )
    @NotNull(message = "Category is required")
    private BookCategory category;
    
    @Schema(
        description = "Book description",
        example = "A comprehensive guide to writing clean, maintainable code",
        maxLength = 1000
    )
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Schema(
        description = "URL of the book cover image",
        example = "https://images.bookstore.com/covers/clean-code.jpg",
        format = "uri"
    )
    @URL(message = "Cover image URL must be valid")
    private String coverImageUrl;
    
    @Schema(
        description = "Whether the book is available for purchase",
        example = "true",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Boolean available;
    
    @Schema(
        description = "Timestamp when the book was created",
        example = "2024-01-15T10:30:00",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime createdAt;
    
    @Schema(
        description = "Timestamp when the book was last updated",
        example = "2024-01-15T10:30:00",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime updatedAt;
}

@Schema(description = "Request object for creating a new book")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookRequest {
    
    @Schema(description = "Title of the book", required = true)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Schema(description = "Author of the book", required = true)
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    private String author;
    
    @Schema(description = "ISBN number", required = true)
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^\\d{13}$", message = "ISBN must be 13 digits")
    private String isbn;
    
    @Schema(description = "Price in USD", required = true, minimum = "0")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;
    
    @Schema(description = "Initial stock quantity", required = true, minimum = "0")
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be non-negative")
    private Integer stock;
    
    @Schema(description = "Book category", required = true)
    @NotNull(message = "Category is required")
    private BookCategory category;
    
    @Schema(description = "Book description")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Schema(description = "Cover image URL")
    @URL(message = "Cover image URL must be valid")
    private String coverImageUrl;
}
```

### 3. Error Response Documentation
```java
@Schema(description = "Error response object")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    
    @Schema(
        description = "Timestamp when the error occurred",
        example = "2024-01-15T10:30:00.123Z"
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "HTTP status code",
        example = "404"
    )
    private int status;
    
    @Schema(
        description = "Error type/category",
        example = "Not Found"
    )
    private String error;
    
    @Schema(
        description = "Detailed error message",
        example = "Book not found with id: 123"
    )
    private String message;
    
    @Schema(
        description = "Request path that caused the error",
        example = "/api/books/123"
    )
    private String path;
    
    @Schema(
        description = "Validation errors (if applicable)"
    )
    private List<ValidationError> validationErrors;
    
    @Schema(description = "Field-specific validation error")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        
        @Schema(description = "Field name", example = "title")
        private String field;
        
        @Schema(description = "Error message", example = "Title is required")
        private String message;
        
        @Schema(description = "Rejected value", example = "")
        private Object rejectedValue;
    }
}
```

### 4. Custom Swagger Configurations

#### Hide Sensitive Endpoints
```java
@RestController
@Hidden // Hide entire controller from Swagger
public class InternalController {
    // Internal endpoints not shown in documentation
}

public class AdminController {
    
    @Operation(hidden = true) // Hide specific operation
    @GetMapping("/internal/reset")
    public ResponseEntity<Void> resetData() {
        // Hidden from documentation
        return ResponseEntity.ok().build();
    }
}
```

#### Custom Response Examples
```java
@Operation(summary = "Search books")
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Search results",
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "Empty results",
                    description = "When no books match the criteria",
                    value = """
                        {
                          "content": [],
                          "pageable": {
                            "page": 0,
                            "size": 10,
                            "totalElements": 0,
                            "totalPages": 0
                          }
                        }
                        """
                ),
                @ExampleObject(
                    name = "With results",
                    description = "When books are found",
                    value = """
                        {
                          "content": [
                            {
                              "id": 1,
                              "title": "Spring Boot in Action",
                              "author": "Craig Walls",
                              "price": 449.99,
                              "category": "TECHNOLOGY"
                            }
                          ],
                          "pageable": {
                            "page": 0,
                            "size": 10,
                            "totalElements": 1,
                            "totalPages": 1
                          }
                        }
                        """
                )
            }
        )
    )
})
@GetMapping("/search")
public ResponseEntity<Page<BookDTO>> searchBooks(
        @RequestParam String query) {
    // Implementation
}
```

## Advanced Documentation Features

### 1. API Versioning Documentation
```java
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books V1", description = "Books API Version 1.0")
public class BookControllerV1 {
    // V1 implementation
}

@RestController
@RequestMapping("/api/v2/books")
@Tag(name = "Books V2", description = "Books API Version 2.0 (Latest)")
public class BookControllerV2 {
    // V2 implementation with breaking changes
}
```

### 2. Authentication Documentation
```java
@SecuritySchemes({
    @SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
    ),
    @SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-KEY"
    )
})
@Configuration
public class SecuritySchemeConfig {
    // Configuration
}
```

### 3. Webhook Documentation
```java
@Operation(
    summary = "Book Event Webhook",
    description = """
        This webhook will be called when book events occur.
        
        ### Events:
        - `book.created` - When a new book is added
        - `book.updated` - When book details are modified
        - `book.deleted` - When a book is removed
        
        ### Payload:
        The webhook payload will contain the event type and book data.
        
        ### Security:
        Webhooks are signed with HMAC-SHA256 using your webhook secret.
        Verify the signature using the `X-Webhook-Signature` header.
        """,
    requestBody = @RequestBody(
        description = "Webhook payload",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = WebhookPayload.class)
        )
    )
)
```

## Testing with Swagger UI

### 1. Authentication Setup
```javascript
// Custom JavaScript for Swagger UI
window.onload = function() {
    // Auto-populate JWT token from localStorage
    const token = localStorage.getItem('jwt_token');
    if (token) {
        ui.preauthorizeApiKey('bearerAuth', `Bearer ${token}`);
    }
    
    // Add custom styling
    const style = document.createElement('style');
    style.textContent = `
        .swagger-ui .topbar { background-color: #2c3e50; }
        .swagger-ui .topbar .download-url-wrapper .download-url-button {
            background-color: #34495e;
        }
    `;
    document.head.appendChild(style);
};
```

### 2. Environment Configuration
```yaml
# application-dev.yml
springdoc:
  swagger-ui:
    # Pre-populate server URL
    servers:
      - url: http://localhost:8081
        description: Local Development
    
    # Default authentication
    oauth:
      client-id: bookstore-client
      client-secret: secret
      realm: bookstore
    
    # Custom CSS
    custom-css-url: /css/swagger-custom.css
```

## Documentation Best Practices

### 1. Comprehensive Examples
```java
@Operation(
    summary = "Complex search with multiple filters",
    description = """
        Advanced book search supporting multiple criteria:
        
        ### Supported Filters:
        - **category**: Filter by book category
        - **search**: Search in title and author
        - **priceRange**: Filter by price range
        - **availability**: Only available books
        - **rating**: Minimum rating filter
        
        ### Sorting Options:
        - `title` - Sort by title (A-Z)
        - `price` - Sort by price (low to high)
        - `rating` - Sort by rating (high to low)
        - `created` - Sort by creation date (newest first)
        
        ### Usage Examples:
        - Search for technology books: `?category=TECHNOLOGY`
        - Find books under $300: `?maxPrice=300`
        - Search for "Spring": `?search=Spring&category=TECHNOLOGY`
        """,
    parameters = {
        @Parameter(
            name = "sort",
            description = "Sort criteria",
            examples = {
                @ExampleObject(name = "By title", value = "title"),
                @ExampleObject(name = "By price", value = "price"),
                @ExampleObject(name = "By rating", value = "rating,desc")
            }
        )
    }
)
```

### 2. Error Scenarios Documentation
```java
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(
        responseCode = "400", 
        description = "Bad Request",
        content = @Content(
            examples = {
                @ExampleObject(
                    name = "Invalid page parameter",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Page number must be non-negative",
                          "path": "/api/books"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Validation errors",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 400,
                          "error": "Validation Failed",
                          "message": "Request validation failed",
                          "path": "/api/books",
                          "validationErrors": [
                            {
                              "field": "title",
                              "message": "Title is required",
                              "rejectedValue": ""
                            }
                          ]
                        }
                        """
                )
            }
        )
    ),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
})
```

## Interview Questions & Answers

### Q1: What are the benefits of using Swagger/OpenAPI for API documentation?

**Answer**:
- **Interactive documentation**: Test APIs directly from browser
- **Code generation**: Generate client SDKs automatically
- **API-first development**: Design APIs before implementation
- **Consistency**: Standardized documentation format
- **Validation**: Request/response validation
- **Team collaboration**: Clear contract between frontend and backend

### Q2: How do you handle API versioning in Swagger documentation?

**Answer**:
1. **URL versioning**: Different endpoints (/v1, /v2)
2. **Header versioning**: Version in Accept header
3. **Parameter versioning**: Version as query parameter
4. **Separate documentation**: Different Swagger docs per version
5. **Deprecation notices**: Mark old versions as deprecated

### Q3: How do you secure Swagger UI in production?

**Answer**:
1. **Disable in production**: Set `springdoc.swagger-ui.enabled=false`
2. **IP whitelisting**: Restrict access to specific IPs
3. **Authentication**: Require login to access documentation
4. **Separate environment**: Deploy docs to internal network only
5. **Reverse proxy**: Use nginx to control access

### Q4: What's the difference between @Schema and @Parameter annotations?

**Answer**:
- **@Schema**: Documents data models, DTOs, request/response bodies
- **@Parameter**: Documents query parameters, path variables, headers
- **@Schema** is for complex objects, **@Parameter** is for simple values
- Both support examples, descriptions, and validation constraints

## Best Practices

1. **Provide comprehensive examples** for all endpoints
2. **Document error scenarios** with example responses
3. **Use meaningful descriptions** not just field names
4. **Include authentication requirements** clearly
5. **Version your APIs** and document breaking changes
6. **Validate documentation** matches actual implementation
7. **Use groups** to organize related endpoints
8. **Secure production documentation** appropriately

## Next Steps

Continue to [Design Patterns](08-DesignPatterns.md) to learn about implementing common design patterns in Spring Boot applications.
