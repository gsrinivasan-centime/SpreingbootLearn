package com.bookstore.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final RedissonClient redissonClient;
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    
    /**
     * Check if operation with given key has already been processed
     * @param key the idempotency key
     * @return true if operation already processed, false otherwise
     */
    public boolean isProcessed(String key) {
        RMapCache<String, String> cache = redissonClient.getMapCache("idempotency");
        String fullKey = IDEMPOTENCY_KEY_PREFIX + key;
        boolean exists = cache.containsKey(fullKey);
        
        if (exists) {
            log.info("Duplicate operation detected for key: {}", key);
        }
        
        return exists;
    }
    
    /**
     * Mark operation as processed
     * @param key the idempotency key
     * @param result the operation result (optional)
     */
    public void markAsProcessed(String key, String result) {
        RMapCache<String, String> cache = redissonClient.getMapCache("idempotency");
        String fullKey = IDEMPOTENCY_KEY_PREFIX + key;
        
        cache.put(fullKey, result != null ? result : "processed", 
                IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
        
        log.debug("Marked operation as processed for key: {}", key);
    }
    
    /**
     * Get the result of a previously processed operation
     * @param key the idempotency key
     * @return the stored result, or null if not found
     */
    public String getProcessedResult(String key) {
        RMapCache<String, String> cache = redissonClient.getMapCache("idempotency");
        String fullKey = IDEMPOTENCY_KEY_PREFIX + key;
        return cache.get(fullKey);
    }
    
    /**
     * Generate idempotency key for user creation
     * @param email the user email
     * @param phoneNumber the user phone number
     * @return the idempotency key
     */
    public String generateCreateUserKey(String email, String phoneNumber) {
        return "create_user:" + email + ":" + phoneNumber.hashCode();
    }
    
    /**
     * Generate idempotency key for user update
     * @param userId the user ID
     * @param requestHash the hash of the update request
     * @return the idempotency key
     */
    public String generateUpdateUserKey(Long userId, String requestHash) {
        return "update_user:" + userId + ":" + requestHash;
    }
}
