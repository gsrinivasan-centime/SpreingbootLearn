package com.bookstore.bookservice.service;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.dto.UpdateBookRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface BookService {

    // CRUD Operations
    BookDto createBook(CreateBookRequestDto createBookRequest);
    BookDto getBookById(Long id);
    BookDto getBookByIsbn(String isbn);
    List<BookDto> getAllBooks();
    Page<BookDto> getAllBooks(Pageable pageable);
    BookDto updateBook(Long id, UpdateBookRequestDto updateBookRequest);
    void deleteBook(Long id);
    void softDeleteBook(Long id);
    void reactivateBook(Long id);

    // Search Operations
    List<BookDto> searchBooksByTitle(String title);
    List<BookDto> searchBooksByAuthor(String author);
    List<BookDto> searchBooksByCategory(String category);
    Page<BookDto> searchBooks(String searchTerm, Pageable pageable);
    Page<BookDto> findBooksWithFilters(String title, String author, String category, 
                                       BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Business Operations
    BookDto updateStock(Long bookId, Integer quantity);
    List<BookDto> findBooksWithLowStock(Integer threshold);
    List<BookDto> findAvailableBooks();
    List<BookDto> findBooksByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    
    // Batch Operations
    List<BookDto> createBooksInBatch(List<CreateBookRequestDto> createBookRequests);
    void updateStockInBatch(List<Long> bookIds, List<Integer> quantities);

    // Analytics
    Long countBooksByCategory(String category);
    List<BookDto> getRecentBooks(int limit);

    // Idempotent Operations
    BookDto createBookIdempotent(String idempotencyKey, CreateBookRequestDto createBookRequest);
    BookDto updateBookIdempotent(String idempotencyKey, Long id, UpdateBookRequestDto updateBookRequest);
}
