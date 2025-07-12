package com.bookstore.bookservice.mapper;

import com.bookstore.bookservice.dto.BookDto;
import com.bookstore.bookservice.dto.CreateBookRequestDto;
import com.bookstore.bookservice.dto.UpdateBookRequestDto;
import com.bookstore.bookservice.entity.Book;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookMapper {

    BookDto toDto(Book book);

    Book toEntity(BookDto bookDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Book toEntity(CreateBookRequestDto createBookRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isbn", ignore = true) // ISBN should not be updatable
    void updateEntityFromDto(UpdateBookRequestDto updateBookRequestDto, @MappingTarget Book book);
}
