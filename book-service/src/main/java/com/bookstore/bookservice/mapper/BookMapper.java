package com.bookstore.bookservice.mapper;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.dto.UpdateBookRequestDto;
import com.bookstore.bookservice.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }

        BookDto bookDto = new BookDto();
        bookDto.setId(book.getId());
        bookDto.setTitle(book.getTitle());
        bookDto.setAuthor(book.getAuthor());
        bookDto.setIsbn(book.getIsbn());
        bookDto.setDescription(book.getDescription());
        bookDto.setPrice(book.getPrice());
        bookDto.setStockQuantity(book.getStockQuantity());
        bookDto.setCategory(book.getCategory());
        bookDto.setPublisher(book.getPublisher());
        bookDto.setPublicationYear(book.getPublicationYear());
        bookDto.setLanguage(book.getLanguage());
        bookDto.setPages(book.getPages());
        bookDto.setImageUrl(book.getImageUrl());
        bookDto.setActive(book.getActive());
        bookDto.setCreatedAt(book.getCreatedAt());
        bookDto.setUpdatedAt(book.getUpdatedAt());
        
        return bookDto;
    }

    public Book toEntity(BookDto bookDto) {
        if (bookDto == null) {
            return null;
        }

        Book book = new Book();
        book.setId(bookDto.getId());
        book.setTitle(bookDto.getTitle());
        book.setAuthor(bookDto.getAuthor());
        book.setIsbn(bookDto.getIsbn());
        book.setDescription(bookDto.getDescription());
        book.setPrice(bookDto.getPrice());
        book.setStockQuantity(bookDto.getStockQuantity());
        book.setCategory(bookDto.getCategory());
        book.setPublisher(bookDto.getPublisher());
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setLanguage(bookDto.getLanguage());
        book.setPages(bookDto.getPages());
        book.setImageUrl(bookDto.getImageUrl());
        book.setActive(bookDto.getActive());
        
        return book;
    }

    public Book toEntity(CreateBookRequestDto createBookRequestDto) {
        if (createBookRequestDto == null) {
            return null;
        }

        Book book = new Book();
        book.setTitle(createBookRequestDto.getTitle());
        book.setAuthor(createBookRequestDto.getAuthor());
        book.setIsbn(createBookRequestDto.getIsbn());
        book.setDescription(createBookRequestDto.getDescription());
        book.setPrice(createBookRequestDto.getPrice());
        book.setStockQuantity(createBookRequestDto.getStockQuantity());
        book.setCategory(createBookRequestDto.getCategory());
        book.setPublisher(createBookRequestDto.getPublisher());
        book.setPublicationYear(createBookRequestDto.getPublicationYear());
        book.setLanguage(createBookRequestDto.getLanguage());
        book.setPages(createBookRequestDto.getPages());
        book.setImageUrl(createBookRequestDto.getImageUrl());
        book.setActive(true);
        
        return book;
    }

    public void updateEntityFromDto(UpdateBookRequestDto updateBookRequestDto, Book book) {
        if (updateBookRequestDto == null || book == null) {
            return;
        }
        
        if (updateBookRequestDto.getTitle() != null) {
            book.setTitle(updateBookRequestDto.getTitle());
        }
        if (updateBookRequestDto.getAuthor() != null) {
            book.setAuthor(updateBookRequestDto.getAuthor());
        }
        if (updateBookRequestDto.getDescription() != null) {
            book.setDescription(updateBookRequestDto.getDescription());
        }
        if (updateBookRequestDto.getPrice() != null) {
            book.setPrice(updateBookRequestDto.getPrice());
        }
        if (updateBookRequestDto.getStockQuantity() != null) {
            book.setStockQuantity(updateBookRequestDto.getStockQuantity());
        }
        if (updateBookRequestDto.getCategory() != null) {
            book.setCategory(updateBookRequestDto.getCategory());
        }
        if (updateBookRequestDto.getPublisher() != null) {
            book.setPublisher(updateBookRequestDto.getPublisher());
        }
        if (updateBookRequestDto.getPublicationYear() != null) {
            book.setPublicationYear(updateBookRequestDto.getPublicationYear());
        }
        if (updateBookRequestDto.getLanguage() != null) {
            book.setLanguage(updateBookRequestDto.getLanguage());
        }
        if (updateBookRequestDto.getPages() != null) {
            book.setPages(updateBookRequestDto.getPages());
        }
        if (updateBookRequestDto.getImageUrl() != null) {
            book.setImageUrl(updateBookRequestDto.getImageUrl());
        }
        if (updateBookRequestDto.getActive() != null) {
            book.setActive(updateBookRequestDto.getActive());
        }
    }
}
