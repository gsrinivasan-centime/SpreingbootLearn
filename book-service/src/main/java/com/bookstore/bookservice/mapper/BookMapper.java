package com.bookstore.bookservice.mapper;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.dto.UpdateBookRequestDto;
import com.bookstore.bookservice.entity.Book;

public interface BookMapper {

    BookDto toDto(Book book);
    
    Book toEntity(BookDto bookDto);
    
    Book toEntity(CreateBookRequestDto createBookRequestDto);
    
    void updateEntityFromDto(UpdateBookRequestDto updateBookRequestDto, Book book);
}
