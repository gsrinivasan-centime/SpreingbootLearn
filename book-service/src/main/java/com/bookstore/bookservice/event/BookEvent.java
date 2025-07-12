package com.bookstore.bookservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BookEvent implements Serializable {
    
    private String eventType;
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private Integer stockQuantity;
    private LocalDateTime timestamp;
    
    public BookEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public BookEvent(String eventType, Long bookId, String title, String author, String isbn, Integer stockQuantity) {
        this.eventType = eventType;
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.stockQuantity = stockQuantity;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Long getBookId() {
        return bookId;
    }
    
    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getIsbn() {
        return isbn;
    }
    
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "BookEvent{" +
                "eventType='" + eventType + '\'' +
                ", bookId=" + bookId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", isbn='" + isbn + '\'' +
                ", stockQuantity=" + stockQuantity +
                ", timestamp=" + timestamp +
                '}';
    }
}
