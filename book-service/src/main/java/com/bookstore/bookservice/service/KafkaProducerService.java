package com.bookstore.bookservice.service;

import com.bookstore.bookservice.event.BookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.book-events}")
    private String bookEventsTopic;

    @Value("${kafka.topics.book-cdc}")
    private String bookCdcTopic;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookEvent(BookEvent bookEvent) {
        logger.info("Publishing book event: {}", bookEvent);
        
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(bookEventsTopic, bookEvent.getBookId().toString(), bookEvent);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published book event for book ID: {} with offset: {}", 
                              bookEvent.getBookId(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish book event for book ID: {}", bookEvent.getBookId(), ex);
                }
            });
        } catch (Exception e) {
            logger.error("Error publishing book event", e);
        }
    }

    public void publishBookCdcEvent(BookEvent bookEvent) {
        logger.info("Publishing book CDC event: {}", bookEvent);
        
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(bookCdcTopic, bookEvent.getBookId().toString(), bookEvent);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published book CDC event for book ID: {} with offset: {}", 
                              bookEvent.getBookId(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish book CDC event for book ID: {}", bookEvent.getBookId(), ex);
                }
            });
        } catch (Exception e) {
            logger.error("Error publishing book CDC event", e);
        }
    }
}
