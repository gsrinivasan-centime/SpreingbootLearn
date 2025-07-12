package com.bookstore.bookservice.repository;

import com.bookstore.bookservice.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Find by ISBN
    Optional<Book> findByIsbn(String isbn);

    // Find by title (case-insensitive)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Find by author (case-insensitive)
    List<Book> findByAuthorContainingIgnoreCase(String author);

    // Find by category
    List<Book> findByCategory(String category);

    // Find by category (case-insensitive)
    List<Book> findByCategoryIgnoreCase(String category);

    // Find active books
    List<Book> findByActiveTrue();

    // Find books in stock
    @Query("SELECT b FROM Book b WHERE b.stockQuantity > 0")
    List<Book> findBooksInStock();

    // Find available books (active and in stock)
    @Query("SELECT b FROM Book b WHERE b.active = true AND b.stockQuantity > 0")
    List<Book> findAvailableBooks();

    // Find books by price range
    List<Book> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Find books by author and category
    List<Book> findByAuthorContainingIgnoreCaseAndCategory(String author, String category);

    // Search books by title, author, or description
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Book> searchBooks(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Find books with low stock
    @Query("SELECT b FROM Book b WHERE b.stockQuantity < :threshold AND b.active = true")
    List<Book> findBooksWithLowStock(@Param("threshold") Integer threshold);

    // Update stock quantity
    @Modifying
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :bookId")
    int updateStockQuantity(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);

    // Find books by multiple categories
    List<Book> findByCategoryIn(List<String> categories);

    // Find books published in a specific year
    List<Book> findByPublicationYear(Integer year);

    // Find books by publication year range
    List<Book> findByPublicationYearBetween(Integer startYear, Integer endYear);

    // Count books by category
    @Query("SELECT COUNT(b) FROM Book b WHERE b.category = :category AND b.active = true")
    Long countBooksByCategory(@Param("category") String category);

    // Find top N books by a criteria (you can extend this)
    @Query("SELECT b FROM Book b WHERE b.active = true ORDER BY b.createdAt DESC")
    List<Book> findRecentBooks(Pageable pageable);

    // Custom query for complex search with pagination
    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:category IS NULL OR b.category = :category) AND " +
           "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR b.price <= :maxPrice) AND " +
           "b.active = true")
    Page<Book> findBooksWithFilters(
        @Param("title") String title,
        @Param("author") String author,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    // Check if ISBN exists
    boolean existsByIsbn(String isbn);

    // Find books by publisher
    List<Book> findByPublisherContainingIgnoreCase(String publisher);

    // Soft delete (mark as inactive)
    @Modifying
    @Query("UPDATE Book b SET b.active = false WHERE b.id = :bookId")
    int softDeleteBook(@Param("bookId") Long bookId);

    // Reactivate book
    @Modifying
    @Query("UPDATE Book b SET b.active = true WHERE b.id = :bookId")
    int reactivateBook(@Param("bookId") Long bookId);
}
