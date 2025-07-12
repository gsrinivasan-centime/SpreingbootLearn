package com.bookstore.userservice.service;

import com.bookstore.userservice.entity.User;
import com.bookstore.userservice.event.UserEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;
    
    public void publishUserCreatedEvent(User user) {
        publishUserEvent(user, "USER_CREATED");
    }
    
    public void publishUserUpdatedEvent(User user) {
        publishUserEvent(user, "USER_UPDATED");
    }
    
    public void publishUserActivatedEvent(User user) {
        publishUserEvent(user, "USER_ACTIVATED");
    }
    
    public void publishUserDeactivatedEvent(User user) {
        publishUserEvent(user, "USER_DEACTIVATED");
    }
    
    public void publishUserDeletedEvent(User user) {
        publishUserEvent(user, "USER_DELETED");
    }
    
    private void publishUserEvent(User user, String eventType) {
        try {
            UserEvent event = UserEvent.builder()
                    .userId(user.getId())
                    .eventType(eventType)
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .active(user.getActive())
                    .timestamp(LocalDateTime.now())
                    .build();
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(userEventsTopic, user.getId().toString(), eventJson);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Published user event: {} for user ID: {}", eventType, user.getId());
                } else {
                    log.error("Failed to publish user event: {} for user ID: {}", 
                            eventType, user.getId(), exception);
                }
            });
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing user event for user ID: {}", user.getId(), e);
        }
    }
}
