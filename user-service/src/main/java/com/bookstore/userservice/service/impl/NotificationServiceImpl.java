package com.bookstore.userservice.service.impl;

import com.bookstore.userservice.dto.BookEventDTO;
import com.bookstore.userservice.repository.UserRepository;
import com.bookstore.userservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final UserRepository userRepository;
    
    @Override
    public void notifyNewBookAdded(BookEventDTO bookEvent) {
        log.info("Would send notification about new book: {} by author: {}", 
                 bookEvent.getTitle(), bookEvent.getAuthor());
        
        // In a real application, you would:
        // 1. Find users who might be interested in this book (based on preferences, genre, etc.)
        // 2. Send them a notification (email, push notification, etc.)
        
        // For example:
        // List<User> interestedUsers = userRepository.findByPreferredAuthor(bookEvent.getAuthor());
        // emailService.sendBulkEmail(interestedUsers, "New book by your favorite author!", 
        //     "Check out " + bookEvent.getTitle() + " by " + bookEvent.getAuthor());
    }
    
    @Override
    public void notifyBookBackInStock(BookEventDTO bookEvent) {
        log.info("Would send notification that book is back in stock: {}", bookEvent.getTitle());
        
        // In a real application, you would:
        // 1. Find users who have waitlisted this book
        // 2. Send them a notification
        
        // For example:
        // List<User> waitlistedUsers = userRepository.findByWaitlistedBookId(bookEvent.getBookId());
        // emailService.sendBulkEmail(waitlistedUsers, "Book back in stock!", 
        //     bookEvent.getTitle() + " is now available. Hurry before it's gone!");
    }
}
