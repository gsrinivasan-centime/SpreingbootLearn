package com.bookstore.userservice.service;

import com.bookstore.userservice.dto.BookEventDTO;

public interface NotificationService {
    
    /**
     * Sends a notification about a newly added book
     * 
     * @param bookEvent The book event containing book details
     */
    void notifyNewBookAdded(BookEventDTO bookEvent);
    
    /**
     * Sends a notification that a book is back in stock
     * 
     * @param bookEvent The book event containing book details
     */
    void notifyBookBackInStock(BookEventDTO bookEvent);
}
