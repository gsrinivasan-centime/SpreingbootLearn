package com.bookstore.userservice.service;

import com.bookstore.userservice.dto.BookEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookEventConsumerService {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    
    @KafkaListener(topics = "${kafka.topics.book-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeBookEvent(String eventJson, Acknowledgment ack) {
        try {
            log.info("Received book event: {}", eventJson);
            
            BookEventDTO bookEvent = objectMapper.readValue(eventJson, BookEventDTO.class);
            
            // Process the event based on event type
            switch (bookEvent.getEventType()) {
                case "BOOK_CREATED":
                    handleBookCreated(bookEvent);
                    break;
                case "BOOK_UPDATED":
                    handleBookUpdated(bookEvent);
                    break;
                case "BOOK_DELETED":
                    handleBookDeleted(bookEvent);
                    break;
                case "STOCK_UPDATED":
                    handleStockUpdated(bookEvent);
                    break;
                default:
                    log.warn("Unknown book event type: {}", bookEvent.getEventType());
            }
            
            // Acknowledge the message to Kafka
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing book event: {}", eventJson, e);
            // Don't acknowledge the message to trigger redelivery
            // If this consistently fails, it might end up in a DLQ if configured
        }
    }
    
    private void handleBookCreated(BookEventDTO bookEvent) {
        log.info("Processing BOOK_CREATED event for book ID: {}, title: {}", 
                bookEvent.getBookId(), bookEvent.getTitle());
        
        // Example: Notify admins about new book
        notificationService.notifyNewBookAdded(bookEvent);
        
        // Example: You could save this to a local book cache or database
    }
    
    private void handleBookUpdated(BookEventDTO bookEvent) {
        log.info("Processing BOOK_UPDATED event for book ID: {}", bookEvent.getBookId());
        
        // Example: You could update a local book cache or database
    }
    
    private void handleBookDeleted(BookEventDTO bookEvent) {
        log.info("Processing BOOK_DELETED event for book ID: {}", bookEvent.getBookId());
        
        // Example: You could remove from a local book cache or database
        // Example: Check if any users had this book in their wishlist and notify them
    }
    
    private void handleStockUpdated(BookEventDTO bookEvent) {
        log.info("Processing STOCK_UPDATED event for book ID: {}, new quantity: {}", 
                bookEvent.getBookId(), bookEvent.getStockQuantity());
        
        // Example: If book back in stock, notify users who were waiting for it
        if (bookEvent.getStockQuantity() > 0) {
            notificationService.notifyBookBackInStock(bookEvent);
        }
    }
}
