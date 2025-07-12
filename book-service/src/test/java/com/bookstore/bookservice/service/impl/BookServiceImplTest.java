package com.bookstore.bookservice.service.impl;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.dto.UpdateBookRequestDto;
import com.bookstore.bookservice.entity.Book;
import com.bookstore.bookservice.exception.BookNotFoundException;
import com.bookstore.bookservice.exception.DuplicateIsbnException;
import com.bookstore.bookservice.mapper.BookMapper;
import com.bookstore.bookservice.repository.BookRepository;
import com.bookstore.bookservice.service.IdempotencyService;
import com.bookstore.bookservice.service.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private BookDto testBookDto;
    private CreateBookRequestDto createBookRequestDto;
    private UpdateBookRequestDto updateBookRequestDto;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("9781234567890");
        testBook.setPrice(new BigDecimal("19.99"));
        testBook.setStockQuantity(50);
        testBook.setCategory("Fiction");
        testBook.setActive(true);

        testBookDto = new BookDto();
        testBookDto.setId(1L);
        testBookDto.setTitle("Test Book");
        testBookDto.setAuthor("Test Author");
        testBookDto.setIsbn("9781234567890");
        testBookDto.setPrice(new BigDecimal("19.99"));
        testBookDto.setStockQuantity(50);
        testBookDto.setCategory("Fiction");
        testBookDto.setActive(true);

        createBookRequestDto = new CreateBookRequestDto();
        createBookRequestDto.setTitle("New Book");
        createBookRequestDto.setAuthor("New Author");
        createBookRequestDto.setIsbn("9780987654321");
        createBookRequestDto.setPrice(new BigDecimal("29.99"));
        createBookRequestDto.setStockQuantity(25);
        createBookRequestDto.setCategory("Technology");

        updateBookRequestDto = new UpdateBookRequestDto();
        updateBookRequestDto.setTitle("Updated Book");
        updateBookRequestDto.setPrice(new BigDecimal("24.99"));
        updateBookRequestDto.setStockQuantity(75);
    }

    @Test
    void createBook_Success() {
        // Arrange
        when(bookRepository.existsByIsbn(createBookRequestDto.getIsbn())).thenReturn(false);
        when(bookMapper.toEntity(createBookRequestDto)).thenReturn(testBook);
        when(bookRepository.save(testBook)).thenReturn(testBook);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.createBook(createBookRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(testBookDto.getTitle(), result.getTitle());
        assertEquals(testBookDto.getAuthor(), result.getAuthor());
        assertEquals(testBookDto.getIsbn(), result.getIsbn());

        verify(bookRepository).existsByIsbn(createBookRequestDto.getIsbn());
        verify(bookRepository).save(testBook);
        verify(kafkaProducerService).publishBookEvent(any());
    }

    @Test
    void createBook_DuplicateIsbn_ThrowsException() {
        // Arrange
        when(bookRepository.existsByIsbn(createBookRequestDto.getIsbn())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateIsbnException.class, () -> {
            bookService.createBook(createBookRequestDto);
        });

        verify(bookRepository).existsByIsbn(createBookRequestDto.getIsbn());
        verify(bookRepository, never()).save(any());
        verify(kafkaProducerService, never()).publishBookEvent(any());
    }

    @Test
    void getBookById_Success() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.getBookById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testBookDto.getId(), result.getId());
        assertEquals(testBookDto.getTitle(), result.getTitle());

        verify(bookRepository).findById(1L);
    }

    @Test
    void getBookById_NotFound_ThrowsException() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookNotFoundException.class, () -> {
            bookService.getBookById(1L);
        });

        verify(bookRepository).findById(1L);
    }

    @Test
    void getBookByIsbn_Success() {
        // Arrange
        String isbn = "9781234567890";
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(testBook));
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.getBookByIsbn(isbn);

        // Assert
        assertNotNull(result);
        assertEquals(testBookDto.getIsbn(), result.getIsbn());

        verify(bookRepository).findByIsbn(isbn);
    }

    @Test
    void getAllBooks_Success() {
        // Arrange
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByActiveTrue()).thenReturn(books);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        List<BookDto> result = bookService.getAllBooks();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBookDto.getTitle(), result.get(0).getTitle());

        verify(bookRepository).findByActiveTrue();
    }

    @Test
    void getAllBooksWithPagination_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(Arrays.asList(testBook));
        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        Page<BookDto> result = bookService.getAllBooks(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testBookDto.getTitle(), result.getContent().get(0).getTitle());

        verify(bookRepository).findAll(pageable);
    }

    @Test
    void updateBook_Success() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(testBook)).thenReturn(testBook);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.updateBook(1L, updateBookRequestDto);

        // Assert
        assertNotNull(result);

        verify(bookRepository).findById(1L);
        verify(bookMapper).updateEntityFromDto(updateBookRequestDto, testBook);
        verify(bookRepository).save(testBook);
        verify(kafkaProducerService).publishBookEvent(any());
    }

    @Test
    void updateBook_NotFound_ThrowsException() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookNotFoundException.class, () -> {
            bookService.updateBook(1L, updateBookRequestDto);
        });

        verify(bookRepository).findById(1L);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void deleteBook_Success() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act
        bookService.deleteBook(1L);

        // Assert
        verify(bookRepository).findById(1L);
        verify(bookRepository).delete(testBook);
        verify(kafkaProducerService).publishBookEvent(any());
    }

    @Test
    void deleteBook_NotFound_ThrowsException() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookNotFoundException.class, () -> {
            bookService.deleteBook(1L);
        });

        verify(bookRepository).findById(1L);
        verify(bookRepository, never()).delete(any());
    }

    @Test
    void updateStock_Success() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(testBook)).thenReturn(testBook);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.updateStock(1L, 10);

        // Assert
        assertNotNull(result);
        assertEquals(60, testBook.getStockQuantity()); // Original 50 + 10

        verify(bookRepository).findById(1L);
        verify(bookRepository).save(testBook);
        verify(kafkaProducerService).publishBookEvent(any());
    }

    @Test
    void updateStock_InsufficientStock_ThrowsException() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateStock(1L, -100); // Trying to reduce by more than available
        });

        verify(bookRepository).findById(1L);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void searchBooksByTitle_Success() {
        // Arrange
        String title = "Test";
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByTitleContainingIgnoreCase(title)).thenReturn(books);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        List<BookDto> result = bookService.searchBooksByTitle(title);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(bookRepository).findByTitleContainingIgnoreCase(title);
    }

    @Test
    void searchBooksByAuthor_Success() {
        // Arrange
        String author = "Test Author";
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByAuthorContainingIgnoreCase(author)).thenReturn(books);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        List<BookDto> result = bookService.searchBooksByAuthor(author);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(bookRepository).findByAuthorContainingIgnoreCase(author);
    }

    @Test
    void findBooksWithLowStock_Success() {
        // Arrange
        Integer threshold = 10;
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findBooksWithLowStock(threshold)).thenReturn(books);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        List<BookDto> result = bookService.findBooksWithLowStock(threshold);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(bookRepository).findBooksWithLowStock(threshold);
    }

    @Test
    void createBookIdempotent_NewKey_Success() {
        // Arrange
        String idempotencyKey = "test-key-123";
        when(idempotencyService.getCachedResult(idempotencyKey)).thenReturn(Optional.empty());
        when(bookRepository.existsByIsbn(createBookRequestDto.getIsbn())).thenReturn(false);
        when(bookMapper.toEntity(createBookRequestDto)).thenReturn(testBook);
        when(bookRepository.save(testBook)).thenReturn(testBook);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDto);

        // Act
        BookDto result = bookService.createBookIdempotent(idempotencyKey, createBookRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(testBookDto.getTitle(), result.getTitle());

        verify(idempotencyService).getCachedResult(idempotencyKey);
        verify(idempotencyService).cacheResult(idempotencyKey, result);
        verify(bookRepository).save(testBook);
    }

    @Test
    void createBookIdempotent_ExistingKey_ReturnsCachedResult() {
        // Arrange
        String idempotencyKey = "test-key-123";
        when(idempotencyService.getCachedResult(idempotencyKey)).thenReturn(Optional.of(testBookDto));

        // Act
        BookDto result = bookService.createBookIdempotent(idempotencyKey, createBookRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(testBookDto.getTitle(), result.getTitle());

        verify(idempotencyService).getCachedResult(idempotencyKey);
        verify(idempotencyService, never()).cacheResult(any(), any());
        verify(bookRepository, never()).save(any());
    }
}
