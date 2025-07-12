package com.bookstore.bookservice.service;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.entity.Book;
import com.bookstore.bookservice.repository.BookRepository;
import com.bookstore.bookservice.mapper.BookMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class BatchProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingService.class);

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final KafkaProducerService kafkaProducerService;

    @Value("${batch.chunk-size:1000}")
    private int chunkSize;

    @Value("${batch.thread-pool-size:5}")
    private int threadPoolSize;

    @Autowired
    public BatchProcessingService(BookRepository bookRepository, 
                                 BookMapper bookMapper,
                                 KafkaProducerService kafkaProducerService) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Async("batchTaskExecutor")
    @Transactional
    public CompletableFuture<Void> processLowStockBooks() {
        logger.info("Starting batch processing for low stock books");
        
        try {
            List<Book> lowStockBooks = bookRepository.findBooksWithLowStock(5);
            
            if (!lowStockBooks.isEmpty()) {
                logger.warn("Found {} books with low stock", lowStockBooks.size());
                
                // Process in chunks
                for (int i = 0; i < lowStockBooks.size(); i += chunkSize) {
                    int endIndex = Math.min(i + chunkSize, lowStockBooks.size());
                    List<Book> chunk = lowStockBooks.subList(i, endIndex);
                    processLowStockChunk(chunk);
                }
            }
            
            logger.info("Completed batch processing for low stock books");
        } catch (Exception e) {
            logger.error("Error during batch processing of low stock books", e);
            throw e;
        }
        
        return CompletableFuture.completedFuture(null);
    }

    private void processLowStockChunk(List<Book> books) {
        logger.debug("Processing chunk of {} low stock books", books.size());
        
        books.forEach(book -> {
            logger.warn("Low stock alert for book: {} (ISBN: {}) - Current stock: {}", 
                       book.getTitle(), book.getIsbn(), book.getStockQuantity());
            
            // Here you could implement additional logic like:
            // - Send notifications
            // - Create purchase orders
            // - Update stock levels
            // - Send alerts to inventory management
        });
    }

    @Async("batchTaskExecutor")
    @Transactional
    public CompletableFuture<List<BookDto>> bulkUpdateBooks(List<Book> books) {
        logger.info("Starting bulk update for {} books", books.size());
        
        try {
            List<Book> updatedBooks = bookRepository.saveAll(books);
            
            List<BookDto> result = updatedBooks.stream()
                    .map(bookMapper::toDto)
                    .collect(Collectors.toList());
            
            logger.info("Completed bulk update for {} books", updatedBooks.size());
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Error during bulk update of books", e);
            throw e;
        }
    }

    @Async("batchTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> generateInventoryReport() {
        logger.info("Starting inventory report generation");
        
        try {
            List<Book> allBooks = bookRepository.findByActiveTrue();
            
            long totalBooks = allBooks.size();
            long booksInStock = allBooks.stream()
                    .mapToLong(Book::getStockQuantity)
                    .sum();
            long lowStockBooks = bookRepository.findBooksWithLowStock(10).size();
            
            logger.info("Inventory Report - Total Books: {}, Total Stock: {}, Low Stock Books: {}", 
                       totalBooks, booksInStock, lowStockBooks);
            
            // Here you could:
            // - Send the report via email
            // - Store it in a file
            // - Send it to a monitoring system
            // - Publish to Kafka for other services
            
        } catch (Exception e) {
            logger.error("Error generating inventory report", e);
            throw e;
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // Scheduled tasks
    @Scheduled(fixedRate = 3600000) // Every hour
    public void scheduledLowStockCheck() {
        logger.info("Running scheduled low stock check");
        processLowStockBooks();
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void scheduledInventoryReport() {
        logger.info("Running scheduled inventory report");
        generateInventoryReport();
    }

    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public void scheduledCacheWarmup() {
        logger.info("Running scheduled cache warmup");
        warmupCache();
    }

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmupCache() {
        logger.info("Starting cache warmup");
        
        try {
            // Warmup frequently accessed data
            bookRepository.findAvailableBooks();
            bookRepository.findRecentBooks(org.springframework.data.domain.PageRequest.of(0, 20));
            
            // Warmup popular categories
            String[] popularCategories = {"Fiction", "Technology", "Science Fiction", "Biography"};
            for (String category : popularCategories) {
                bookRepository.findByCategoryIgnoreCase(category);
            }
            
            logger.info("Cache warmup completed");
        } catch (Exception e) {
            logger.error("Error during cache warmup", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Scheduled(fixedRate = 7200000) // Every 2 hours
    public void scheduledHealthCheck() {
        logger.info("Running scheduled health check");
        performHealthCheck();
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> performHealthCheck() {
        logger.info("Starting health check");
        
        try {
            // Check database connectivity
            long bookCount = bookRepository.count();
            logger.info("Health check - Database connectivity OK, total books: {}", bookCount);
            
            // Check for data integrity issues
            List<Book> booksWithNegativeStock = bookRepository.findAll().stream()
                    .filter(book -> book.getStockQuantity() < 0)
                    .collect(Collectors.toList());
            
            if (!booksWithNegativeStock.isEmpty()) {
                logger.warn("Health check - Found {} books with negative stock", booksWithNegativeStock.size());
            }
            
            logger.info("Health check completed");
        } catch (Exception e) {
            logger.error("Health check failed", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
}
