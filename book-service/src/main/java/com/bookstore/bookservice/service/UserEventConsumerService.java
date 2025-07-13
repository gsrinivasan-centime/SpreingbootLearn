package com.bookstore.bookservice.service;

import com.bookstore.bookservice.dto.UserEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class UserEventConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserEventConsumerService.class);
    
    private final ObjectMapper objectMapper;
    
    @Autowired
    public UserEventConsumerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "stringKafkaListenerContainerFactory")
    public void consumeUserEvent(String eventJson, Acknowledgment ack) {
        try {
            logger.info("Received user event: {}", eventJson);
            
            UserEventDTO userEvent = objectMapper.readValue(eventJson, UserEventDTO.class);
            
            // Process the event based on event type
            switch (userEvent.getEventType()) {
                case "USER_CREATED":
                    handleUserCreated(userEvent);
                    break;
                case "USER_UPDATED":
                    handleUserUpdated(userEvent);
                    break;
                case "USER_ACTIVATED":
                    handleUserActivated(userEvent);
                    break;
                case "USER_DEACTIVATED":
                    handleUserDeactivated(userEvent);
                    break;
                case "USER_DELETED":
                    handleUserDeleted(userEvent);
                    break;
                default:
                    logger.warn("Unknown user event type: {}", userEvent.getEventType());
            }
            
            // Acknowledge the message to Kafka
            ack.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing user event: {}", eventJson, e);
            // Don't acknowledge the message to trigger redelivery
        }
    }
    
    private void handleUserCreated(UserEventDTO userEvent) {
        logger.info("Processing USER_CREATED event for user ID: {}, name: {} {}", 
                userEvent.getUserId(), userEvent.getFirstName(), userEvent.getLastName());
        
        // Example: You might want to create a local cache of users 
        // or track which users have borrowed books
    }
    
    private void handleUserUpdated(UserEventDTO userEvent) {
        logger.info("Processing USER_UPDATED event for user ID: {}", userEvent.getUserId());
        
        // Example: Update local user cache or related user data
    }
    
    private void handleUserActivated(UserEventDTO userEvent) {
        logger.info("Processing USER_ACTIVATED event for user ID: {}", userEvent.getUserId());
        
        // Example: Enable book borrowing privileges for this user
    }
    
    private void handleUserDeactivated(UserEventDTO userEvent) {
        logger.info("Processing USER_DEACTIVATED event for user ID: {}", userEvent.getUserId());
        
        // Example: Disable book borrowing privileges for this user
        // Check if user has any borrowed books and handle accordingly
    }
    
    private void handleUserDeleted(UserEventDTO userEvent) {
        logger.info("Processing USER_DELETED event for user ID: {}", userEvent.getUserId());
        
        // Example: Remove user from local caches
        // Handle any cleanup needed for this user's data
    }
}
