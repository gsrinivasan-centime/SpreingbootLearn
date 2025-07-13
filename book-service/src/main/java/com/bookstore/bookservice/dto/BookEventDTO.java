package com.bookstore.bookservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookEventDTO implements Serializable {
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("book_id")
    private Long bookId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("author")
    private String author;
    
    @JsonProperty("isbn")
    private String isbn;
    
    @JsonProperty("stock_quantity")
    private Integer stockQuantity;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
