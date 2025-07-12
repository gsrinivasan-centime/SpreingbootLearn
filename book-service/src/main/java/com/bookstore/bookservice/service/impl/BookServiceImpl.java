package com.bookstore.bookservice.service.impl;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.dto.UpdateBookRequestDto;
import com.bookstore.bookservice.entity.Book;
import com.bookstore.bookservice.event.BookEvent;
import com.bookstore.bookservice.exception.BookNotFoundException;
import com.bookstore.bookservice.exception.DuplicateIsbnException;
import com.bookstore.bookservice.exception.IdempotencyException;
import com.bookstore.bookservice.mapper.BookMapper;
import com.bookstore.bookservice.repository.BookRepository;
import com.bookstore.bookservice.service.BookService;
import com.bookstore.bookservice.service.IdempotencyService;
import com.bookstore.bookservice.service.KafkaProducerService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookServiceImpl implements BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final KafkaProducerService kafkaProducerService;
    private final IdempotencyService idempotencyService;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository, 
                          BookMapper bookMapper,
                          KafkaProducerService kafkaProducerService,
                          IdempotencyService idempotencyService) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @CircuitBreaker(name = "book-service", fallbackMethod = "createBookFallback")
    @RateLimiter(name = "book-service")
    @CacheEvict(value = "books", allEntries = true)
    public BookDto createBook(CreateBookRequestDto createBookRequest) {
        logger.info("Creating book with ISBN: {}", createBookRequest.getIsbn());

        // Check if ISBN already exists
        if (bookRepository.existsByIsbn(createBookRequest.getIsbn())) {
            throw new DuplicateIsbnException("Book with ISBN " + createBookRequest.getIsbn() + " already exists");
        }

        Book book = bookMapper.toEntity(createBookRequest);
        Book savedBook = bookRepository.save(book);

        // Publish CDC event
        BookEvent bookEvent = new BookEvent("BOOK_CREATED", savedBook.getId(), 
                                           savedBook.getTitle(), savedBook.getAuthor(), 
                                           savedBook.getIsbn(), savedBook.getStockQuantity());
        kafkaProducerService.publishBookEvent(bookEvent);

        logger.info("Book created successfully with ID: {}", savedBook.getId());
        return bookMapper.toDto(savedBook);
    }

    @Override
    @Cacheable(value = "books", key = "#id")
    @CircuitBreaker(name = "book-service", fallbackMethod = "getBookByIdFallback")
    @Retry(name = "book-service")
    @Transactional(readOnly = true)
    public BookDto getBookById(Long id) {
        logger.debug("Fetching book with ID: {}", id);
        
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));
        
        return bookMapper.toDto(book);
    }

    @Override
    @Cacheable(value = "books", key = "#isbn")
    @CircuitBreaker(name = "book-service", fallbackMethod = "getBookByIsbnFallback")
    @Transactional(readOnly = true)
    public BookDto getBookByIsbn(String isbn) {
        logger.debug("Fetching book with ISBN: {}", isbn);
        
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ISBN: " + isbn));
        
        return bookMapper.toDto(book);
    }

    @Override
    @Cacheable(value = "all-books")
    @CircuitBreaker(name = "book-service", fallbackMethod = "getAllBooksFallback")
    @Transactional(readOnly = true)
    public List<BookDto> getAllBooks() {
        logger.debug("Fetching all books");
        
        List<Book> books = bookRepository.findByActiveTrue();
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookDto> getAllBooks(Pageable pageable) {
        logger.debug("Fetching books with pagination: {}", pageable);
        
        Page<Book> bookPage = bookRepository.findAll(pageable);
        return bookPage.map(bookMapper::toDto);
    }

    @Override
    @CachePut(value = "books", key = "#id")
    @CacheEvict(value = "all-books", allEntries = true)
    @CircuitBreaker(name = "book-service", fallbackMethod = "updateBookFallback")
    public BookDto updateBook(Long id, UpdateBookRequestDto updateBookRequest) {
        logger.info("Updating book with ID: {}", id);
        
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));

        // Update only non-null fields
        bookMapper.updateEntityFromDto(updateBookRequest, existingBook);
        
        Book updatedBook = bookRepository.save(existingBook);

        // Publish CDC event
        BookEvent bookEvent = new BookEvent("BOOK_UPDATED", updatedBook.getId(), 
                                           updatedBook.getTitle(), updatedBook.getAuthor(), 
                                           updatedBook.getIsbn(), updatedBook.getStockQuantity());
        kafkaProducerService.publishBookEvent(bookEvent);

        logger.info("Book updated successfully with ID: {}", updatedBook.getId());
        return bookMapper.toDto(updatedBook);
    }

    @Override
    @CacheEvict(value = {"books", "all-books"}, allEntries = true)
    @CircuitBreaker(name = "book-service", fallbackMethod = "deleteBookFallback")
    public void deleteBook(Long id) {
        logger.info("Deleting book with ID: {}", id);
        
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));

        bookRepository.delete(book);

        // Publish CDC event
        BookEvent bookEvent = new BookEvent("BOOK_DELETED", book.getId(), 
                                           book.getTitle(), book.getAuthor(), 
                                           book.getIsbn(), book.getStockQuantity());
        kafkaProducerService.publishBookEvent(bookEvent);

        logger.info("Book deleted successfully with ID: {}", id);
    }

    @Override
    @CacheEvict(value = {"books", "all-books"}, allEntries = true)
    public void softDeleteBook(Long id) {
        logger.info("Soft deleting book with ID: {}", id);
        
        int updatedRows = bookRepository.softDeleteBook(id);
        if (updatedRows == 0) {
            throw new BookNotFoundException("Book not found with ID: " + id);
        }

        // Publish CDC event
        BookEvent bookEvent = new BookEvent("BOOK_DEACTIVATED", id, null, null, null, null);
        kafkaProducerService.publishBookEvent(bookEvent);

        logger.info("Book soft deleted successfully with ID: {}", id);
    }

    @Override
    @CacheEvict(value = {"books", "all-books"}, allEntries = true)
    public void reactivateBook(Long id) {
        logger.info("Reactivating book with ID: {}", id);
        
        int updatedRows = bookRepository.reactivateBook(id);
        if (updatedRows == 0) {
            throw new BookNotFoundException("Book not found with ID: " + id);
        }

        // Publish CDC event
        BookEvent bookEvent = new BookEvent("BOOK_REACTIVATED", id, null, null, null, null);
        kafkaProducerService.publishBookEvent(bookEvent);

        logger.info("Book reactivated successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDto> searchBooksByTitle(String title) {
        logger.debug("Searching books by title: {}", title);
        
        List<Book> books = bookRepository.findByTitleContainingIgnoreCase(title);
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDto> searchBooksByAuthor(String author) {
        logger.debug("Searching books by author: {}", author);
        
        List<Book> books = bookRepository.findByAuthorContainingIgnoreCase(author);
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDto> searchBooksByCategory(String category) {
        logger.debug("Searching books by category: {}", category);
        
        List<Book> books = bookRepository.findByCategoryIgnoreCase(category);
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookDto> searchBooks(String searchTerm, Pageable pageable) {
        logger.debug("Searching books with term: {}", searchTerm);
        
        Page<Book> bookPage = bookRepository.searchBooks(searchTerm, pageable);
        return bookPage.map(bookMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookDto> findBooksWithFilters(String title, String author, String category, 
                                              BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        logger.debug("Searching books with filters - title: {}, author: {}, category: {}", title, author, category);
        
        Page<Book> bookPage = bookRepository.findBooksWithFilters(title, author, category, minPrice, maxPrice, pageable);
        return bookPage.map(bookMapper::toDto);
    }

    @Override
    @CachePut(value = "books", key = "#bookId")
    @CacheEvict(value = "all-books", allEntries = true)
    public BookDto updateStock(Long bookId, Integer quantity) {
        logger.info("Updating stock for book ID: {} with quantity: {}", bookId, quantity);
        
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + bookId));

        book.updateStock(quantity);
        Book updatedBook = bookRepository.save(book);

        // Publish CDC event
        BookEvent bookEvent = new BookEvent("STOCK_UPDATED", updatedBook.getId(), 
                                           updatedBook.getTitle(), updatedBook.getAuthor(), 
                                           updatedBook.getIsbn(), updatedBook.getStockQuantity());
        kafkaProducerService.publishBookEvent(bookEvent);

        logger.info("Stock updated successfully for book ID: {}", bookId);
        return bookMapper.toDto(updatedBook);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDto> findBooksWithLowStock(Integer threshold) {
        logger.debug("Finding books with low stock threshold: {}", threshold);
        
        List<Book> books = bookRepository.findBooksWithLowStock(threshold);
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "available-books")
    @Transactional(readOnly = true)
    public List<BookDto> findAvailableBooks() {
        logger.debug("Finding available books");
        
        List<Book> books = bookRepository.findAvailableBooks();
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDto> findBooksByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        logger.debug("Finding books in price range: {} - {}", minPrice, maxPrice);
        
        List<Book> books = bookRepository.findByPriceBetween(minPrice, maxPrice);
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "all-books", allEntries = true)
    public List<BookDto> createBooksInBatch(List<CreateBookRequestDto> createBookRequests) {
        logger.info("Creating {} books in batch", createBookRequests.size());
        
        List<Book> books = createBookRequests.stream()
                .map(bookMapper::toEntity)
                .collect(Collectors.toList());
        
        List<Book> savedBooks = bookRepository.saveAll(books);

        // Publish batch CDC event
        savedBooks.forEach(book -> {
            BookEvent bookEvent = new BookEvent("BOOK_CREATED", book.getId(), 
                                               book.getTitle(), book.getAuthor(), 
                                               book.getIsbn(), book.getStockQuantity());
            kafkaProducerService.publishBookEvent(bookEvent);
        });

        logger.info("Batch creation completed for {} books", savedBooks.size());
        return savedBooks.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = {"books", "all-books"}, allEntries = true)
    public void updateStockInBatch(List<Long> bookIds, List<Integer> quantities) {
        logger.info("Updating stock in batch for {} books", bookIds.size());
        
        if (bookIds.size() != quantities.size()) {
            throw new IllegalArgumentException("Book IDs and quantities lists must have the same size");
        }

        for (int i = 0; i < bookIds.size(); i++) {
            updateStock(bookIds.get(i), quantities.get(i));
        }

        logger.info("Batch stock update completed");
    }

    @Override
    @Transactional(readOnly = true)
    public Long countBooksByCategory(String category) {
        logger.debug("Counting books by category: {}", category);
        return bookRepository.countBooksByCategory(category);
    }

    @Override
    @Cacheable(value = "recent-books", key = "#limit")
    @Transactional(readOnly = true)
    public List<BookDto> getRecentBooks(int limit) {
        logger.debug("Getting recent {} books", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> books = bookRepository.findRecentBooks(pageable);
        return books.stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BookDto createBookIdempotent(String idempotencyKey, CreateBookRequestDto createBookRequest) {
        logger.info("Creating book with idempotency key: {}", idempotencyKey);
        
        Optional<Object> cachedResult = idempotencyService.getCachedResult(idempotencyKey);
        if (cachedResult.isPresent()) {
            logger.info("Returning cached result for idempotency key: {}", idempotencyKey);
            return (BookDto) cachedResult.get();
        }

        try {
            BookDto result = createBook(createBookRequest);
            idempotencyService.cacheResult(idempotencyKey, result);
            return result;
        } catch (Exception e) {
            logger.error("Error creating book with idempotency key: {}", idempotencyKey, e);
            throw new IdempotencyException("Failed to create book idempotently", e);
        }
    }

    @Override
    public BookDto updateBookIdempotent(String idempotencyKey, Long id, UpdateBookRequestDto updateBookRequest) {
        logger.info("Updating book with idempotency key: {}", idempotencyKey);
        
        Optional<Object> cachedResult = idempotencyService.getCachedResult(idempotencyKey);
        if (cachedResult.isPresent()) {
            logger.info("Returning cached result for idempotency key: {}", idempotencyKey);
            return (BookDto) cachedResult.get();
        }

        try {
            BookDto result = updateBook(id, updateBookRequest);
            idempotencyService.cacheResult(idempotencyKey, result);
            return result;
        } catch (Exception e) {
            logger.error("Error updating book with idempotency key: {}", idempotencyKey, e);
            throw new IdempotencyException("Failed to update book idempotently", e);
        }
    }

    // Fallback methods for Circuit Breaker
    public BookDto createBookFallback(CreateBookRequestDto createBookRequest, Exception ex) {
        logger.error("Circuit breaker activated for createBook", ex);
        throw new RuntimeException("Book service is currently unavailable. Please try again later.");
    }

    public BookDto getBookByIdFallback(Long id, Exception ex) {
        logger.error("Circuit breaker activated for getBookById", ex);
        throw new RuntimeException("Book service is currently unavailable. Please try again later.");
    }

    public BookDto getBookByIsbnFallback(String isbn, Exception ex) {
        logger.error("Circuit breaker activated for getBookByIsbn", ex);
        throw new RuntimeException("Book service is currently unavailable. Please try again later.");
    }

    public List<BookDto> getAllBooksFallback(Exception ex) {
        logger.error("Circuit breaker activated for getAllBooks", ex);
        throw new RuntimeException("Book service is currently unavailable. Please try again later.");
    }

    public BookDto updateBookFallback(Long id, UpdateBookRequestDto updateBookRequest, Exception ex) {
        logger.error("Circuit breaker activated for updateBook", ex);
        throw new RuntimeException("Book service is currently unavailable. Please try again later.");
    }

    public void deleteBookFallback(Long id, Exception ex) {
        logger.error("Circuit breaker activated for deleteBook", ex);
        throw new RuntimeException("Book service is currently unavailable. Please try again later.");
    }
}
