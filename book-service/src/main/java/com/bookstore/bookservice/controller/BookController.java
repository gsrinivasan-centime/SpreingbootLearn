package com.bookstore.bookservice.controller;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.dto.UpdateBookRequestDto;
import com.bookstore.bookservice.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/books")
@Tag(name = "Book Management", description = "APIs for managing books in the bookstore")
@CrossOrigin(origins = "*")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @Operation(summary = "Create a new book", description = "Creates a new book in the bookstore")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Book created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Book with ISBN already exists")
    })
    public ResponseEntity<BookDto> createBook(
            @Valid @RequestBody CreateBookRequestDto createBookRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        logger.info("Creating book with title: {}", createBookRequest.getTitle());
        
        BookDto createdBook = idempotencyKey != null 
            ? bookService.createBookIdempotent(idempotencyKey, createBookRequest)
            : bookService.createBook(createBookRequest);
        
        return new ResponseEntity<>(createdBook, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Retrieves a book by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book found"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookDto> getBookById(
            @Parameter(description = "Book ID") @PathVariable Long id) {
        
        logger.debug("Fetching book with ID: {}", id);
        BookDto book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Get book by ISBN", description = "Retrieves a book by its ISBN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book found"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookDto> getBookByIsbn(
            @Parameter(description = "Book ISBN") @PathVariable String isbn) {
        
        logger.debug("Fetching book with ISBN: {}", isbn);
        BookDto book = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(book);
    }

    @GetMapping
    @Operation(summary = "Get all books", description = "Retrieves all books with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    })
    public ResponseEntity<Page<BookDto>> getAllBooks(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir) {
        
        logger.debug("Fetching all books - page: {}, size: {}", page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BookDto> books = bookService.getAllBooks(pageable);
        
        return ResponseEntity.ok(books);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update book", description = "Updates an existing book")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Book updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookDto> updateBook(
            @Parameter(description = "Book ID") @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequestDto updateBookRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        logger.info("Updating book with ID: {}", id);
        
        BookDto updatedBook = idempotencyKey != null
            ? bookService.updateBookIdempotent(idempotencyKey, id, updateBookRequest)
            : bookService.updateBook(id, updateBookRequest);
        
        return ResponseEntity.ok(updatedBook);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete book", description = "Deletes a book by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "Book ID") @PathVariable Long id) {
        
        logger.info("Deleting book with ID: {}", id);
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    @Operation(summary = "Soft delete book", description = "Marks a book as inactive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Book soft deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Void> softDeleteBook(
            @Parameter(description = "Book ID") @PathVariable Long id) {
        
        logger.info("Soft deleting book with ID: {}", id);
        bookService.softDeleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate book", description = "Reactivates a soft-deleted book")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Book reactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Void> reactivateBook(
            @Parameter(description = "Book ID") @PathVariable Long id) {
        
        logger.info("Reactivating book with ID: {}", id);
        bookService.reactivateBook(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update book stock", description = "Updates the stock quantity of a book")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid stock quantity"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookDto> updateStock(
            @Parameter(description = "Book ID") @PathVariable Long id,
            @Parameter(description = "Quantity to add/subtract") @RequestParam Integer quantity) {
        
        logger.info("Updating stock for book ID: {} with quantity: {}", id, quantity);
        BookDto updatedBook = bookService.updateStock(id, quantity);
        return ResponseEntity.ok(updatedBook);
    }

    @GetMapping("/search")
    @Operation(summary = "Search books", description = "Searches books by title, author, or description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<Page<BookDto>> searchBooks(
            @Parameter(description = "Search term") @RequestParam String q,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        logger.debug("Searching books with term: {}", q);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BookDto> books = bookService.searchBooks(q, pageable);
        
        return ResponseEntity.ok(books);
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter books", description = "Filters books by various criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filter completed successfully")
    })
    public ResponseEntity<Page<BookDto>> filterBooks(
            @Parameter(description = "Title filter") @RequestParam(required = false) String title,
            @Parameter(description = "Author filter") @RequestParam(required = false) String author,
            @Parameter(description = "Category filter") @RequestParam(required = false) String category,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        logger.debug("Filtering books with criteria - title: {}, author: {}, category: {}", title, author, category);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BookDto> books = bookService.findBooksWithFilters(title, author, category, minPrice, maxPrice, pageable);
        
        return ResponseEntity.ok(books);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get books by category", description = "Retrieves books by category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    })
    public ResponseEntity<List<BookDto>> getBooksByCategory(
            @Parameter(description = "Book category") @PathVariable String category) {
        
        logger.debug("Fetching books by category: {}", category);
        List<BookDto> books = bookService.searchBooksByCategory(category);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/author/{author}")
    @Operation(summary = "Get books by author", description = "Retrieves books by author")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    })
    public ResponseEntity<List<BookDto>> getBooksByAuthor(
            @Parameter(description = "Book author") @PathVariable String author) {
        
        logger.debug("Fetching books by author: {}", author);
        List<BookDto> books = bookService.searchBooksByAuthor(author);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available books", description = "Retrieves all available books (active and in stock)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available books retrieved successfully")
    })
    public ResponseEntity<List<BookDto>> getAvailableBooks() {
        logger.debug("Fetching available books");
        List<BookDto> books = bookService.findAvailableBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get books with low stock", description = "Retrieves books with stock below threshold")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low stock books retrieved successfully")
    })
    public ResponseEntity<List<BookDto>> getBooksWithLowStock(
            @Parameter(description = "Stock threshold") @RequestParam(defaultValue = "10") @Min(1) Integer threshold) {
        
        logger.debug("Fetching books with low stock threshold: {}", threshold);
        List<BookDto> books = bookService.findBooksWithLowStock(threshold);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/price-range")
    @Operation(summary = "Get books by price range", description = "Retrieves books within a price range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    })
    public ResponseEntity<List<BookDto>> getBooksByPriceRange(
            @Parameter(description = "Minimum price") @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam BigDecimal maxPrice) {
        
        logger.debug("Fetching books in price range: {} - {}", minPrice, maxPrice);
        List<BookDto> books = bookService.findBooksByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent books", description = "Retrieves recently added books")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent books retrieved successfully")
    })
    public ResponseEntity<List<BookDto>> getRecentBooks(
            @Parameter(description = "Number of books to retrieve") @RequestParam(defaultValue = "10") @Min(1) int limit) {
        
        logger.debug("Fetching recent {} books", limit);
        List<BookDto> books = bookService.getRecentBooks(limit);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/category/{category}/count")
    @Operation(summary = "Count books by category", description = "Counts books in a specific category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    public ResponseEntity<Long> countBooksByCategory(
            @Parameter(description = "Book category") @PathVariable String category) {
        
        logger.debug("Counting books by category: {}", category);
        Long count = bookService.countBooksByCategory(category);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create books in batch", description = "Creates multiple books in a single operation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Books created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<List<BookDto>> createBooksInBatch(
            @Valid @RequestBody List<CreateBookRequestDto> createBookRequests) {
        
        logger.info("Creating {} books in batch", createBookRequests.size());
        List<BookDto> createdBooks = bookService.createBooksInBatch(createBookRequests);
        return new ResponseEntity<>(createdBooks, HttpStatus.CREATED);
    }
}
