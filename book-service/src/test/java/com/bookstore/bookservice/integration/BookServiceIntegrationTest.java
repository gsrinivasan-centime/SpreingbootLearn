package com.bookstore.bookservice.integration;


import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.entity.Book;
import com.bookstore.bookservice.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
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
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        bookRepository.deleteAll();
    }

    @Test
    void createBook_Success() throws Exception {
        CreateBookRequestDto request = new CreateBookRequestDto();
        request.setTitle("Integration Test Book");
        request.setAuthor("Test Author");
        request.setIsbn("9781234567890");
        request.setPrice(new BigDecimal("29.99"));
        request.setStockQuantity(100);
        request.setCategory("Technology");

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"))
                .andExpect(jsonPath("$.isbn").value("9781234567890"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.stockQuantity").value(100))
                .andExpect(jsonPath("$.category").value("Technology"));
    }

    @Test
    void createBook_DuplicateIsbn_Conflict() throws Exception {
        // First, create a book
        Book existingBook = new Book();
        existingBook.setTitle("Existing Book");
        existingBook.setAuthor("Existing Author");
        existingBook.setIsbn("9781234567890");
        existingBook.setPrice(new BigDecimal("19.99"));
        existingBook.setStockQuantity(50);
        existingBook.setCategory("Fiction");
        bookRepository.save(existingBook);

        // Try to create another book with the same ISBN
        CreateBookRequestDto request = new CreateBookRequestDto();
        request.setTitle("Duplicate Book");
        request.setAuthor("Test Author");
        request.setIsbn("9781234567890"); // Same ISBN
        request.setPrice(new BigDecimal("29.99"));
        request.setStockQuantity(100);
        request.setCategory("Technology");

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getBookById_Success() throws Exception {
        // Create a book first
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setIsbn("9781234567890");
        book.setPrice(new BigDecimal("19.99"));
        book.setStockQuantity(50);
        book.setCategory("Fiction");
        Book savedBook = bookRepository.save(book);

        mockMvc.perform(get("/api/v1/books/{id}", savedBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedBook.getId()))
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"));
    }

    @Test
    void getBookById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllBooks_WithPagination_Success() throws Exception {
        // Create multiple books
        for (int i = 1; i <= 15; i++) {
            Book book = new Book();
            book.setTitle("Book " + i);
            book.setAuthor("Author " + i);
            book.setIsbn("978123456789" + i);
            book.setPrice(new BigDecimal("" + (10 + i)));
            book.setStockQuantity(50);
            book.setCategory("Fiction");
            bookRepository.save(book);
        }

        mockMvc.perform(get("/api/v1/books")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void updateBook_Success() throws Exception {
        // Create a book first
        Book book = new Book();
        book.setTitle("Original Title");
        book.setAuthor("Original Author");
        book.setIsbn("9781234567890");
        book.setPrice(new BigDecimal("19.99"));
        book.setStockQuantity(50);
        book.setCategory("Fiction");
        Book savedBook = bookRepository.save(book);

        String updateJson = """
            {
                "title": "Updated Title",
                "price": 24.99,
                "stockQuantity": 75
            }
            """;

        mockMvc.perform(put("/api/v1/books/{id}", savedBook.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.price").value(24.99))
                .andExpect(jsonPath("$.stockQuantity").value(75))
                .andExpect(jsonPath("$.author").value("Original Author")); // Should remain unchanged
    }

    @Test
    void deleteBook_Success() throws Exception {
        // Create a book first
        Book book = new Book();
        book.setTitle("Book to Delete");
        book.setAuthor("Test Author");
        book.setIsbn("9781234567890");
        book.setPrice(new BigDecimal("19.99"));
        book.setStockQuantity(50);
        book.setCategory("Fiction");
        Book savedBook = bookRepository.save(book);

        mockMvc.perform(delete("/api/v1/books/{id}", savedBook.getId()))
                .andExpect(status().isNoContent());

        // Verify book is deleted
        mockMvc.perform(get("/api/v1/books/{id}", savedBook.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStock_Success() throws Exception {
        // Create a book first
        Book book = new Book();
        book.setTitle("Stock Test Book");
        book.setAuthor("Test Author");
        book.setIsbn("9781234567890");
        book.setPrice(new BigDecimal("19.99"));
        book.setStockQuantity(50);
        book.setCategory("Fiction");
        Book savedBook = bookRepository.save(book);

        mockMvc.perform(patch("/api/v1/books/{id}/stock", savedBook.getId())
                .param("quantity", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(75)); // 50 + 25
    }

    @Test
    void updateStock_InsufficientStock_BadRequest() throws Exception {
        // Create a book first
        Book book = new Book();
        book.setTitle("Stock Test Book");
        book.setAuthor("Test Author");
        book.setIsbn("9781234567890");
        book.setPrice(new BigDecimal("19.99"));
        book.setStockQuantity(50);
        book.setCategory("Fiction");
        Book savedBook = bookRepository.save(book);

        mockMvc.perform(patch("/api/v1/books/{id}/stock", savedBook.getId())
                .param("quantity", "-100")) // Trying to reduce by more than available
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchBooks_Success() throws Exception {
        // Create books with different titles
        Book book1 = new Book();
        book1.setTitle("Java Programming");
        book1.setAuthor("John Doe");
        book1.setIsbn("9781111111111");
        book1.setPrice(new BigDecimal("39.99"));
        book1.setStockQuantity(30);
        book1.setCategory("Technology");
        bookRepository.save(book1);

        Book book2 = new Book();
        book2.setTitle("Python for Beginners");
        book2.setAuthor("Jane Smith");
        book2.setIsbn("9782222222222");
        book2.setPrice(new BigDecimal("29.99"));
        book2.setStockQuantity(40);
        book2.setCategory("Technology");
        bookRepository.save(book2);

        mockMvc.perform(get("/api/v1/books/search")
                .param("q", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Java Programming"));
    }

    @Test
    void filterBooks_Success() throws Exception {
        // Create books in different categories
        Book techBook = new Book();
        techBook.setTitle("Spring Boot Guide");
        techBook.setAuthor("Tech Author");
        techBook.setIsbn("9781111111111");
        techBook.setPrice(new BigDecimal("45.99"));
        techBook.setStockQuantity(20);
        techBook.setCategory("Technology");
        bookRepository.save(techBook);

        Book fictionBook = new Book();
        fictionBook.setTitle("Mystery Novel");
        fictionBook.setAuthor("Fiction Author");
        fictionBook.setIsbn("9782222222222");
        fictionBook.setPrice(new BigDecimal("15.99"));
        fictionBook.setStockQuantity(50);
        fictionBook.setCategory("Fiction");
        bookRepository.save(fictionBook);

        mockMvc.perform(get("/api/v1/books/filter")
                .param("category", "Technology")
                .param("minPrice", "40.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot Guide"));
    }

    @Test
    void idempotentCreate_Success() throws Exception {
        CreateBookRequestDto request = new CreateBookRequestDto();
        request.setTitle("Idempotent Test Book");
        request.setAuthor("Test Author");
        request.setIsbn("9781234567890");
        request.setPrice(new BigDecimal("29.99"));
        request.setStockQuantity(100);
        request.setCategory("Technology");

        String idempotencyKey = "test-idempotency-key-123";

        // First request
        mockMvc.perform(post("/api/v1/books")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Idempotent Test Book"));

        // Second request with same idempotency key - should return same result
        mockMvc.perform(post("/api/v1/books")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Idempotent Test Book"));

        // Verify only one book was created
        long bookCount = bookRepository.count();
        assert bookCount == 1;
    }
}
