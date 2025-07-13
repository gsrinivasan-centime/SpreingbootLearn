package com.bookstore.userservice.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing cache operations
 */
public interface CacheService {
    
    /**
     * Clears the user cache for a specific user ID
     * @param userId The ID of the user whose cache needs to be cleared
     */
    void clearUserCache(Long userId);
    
    /**
     * Clears all caches related to users
     */
    void clearAllUserCaches();
}
