package com.bookstore.userservice.service.impl;

import com.bookstore.userservice.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    
    private final CacheManager cacheManager;
    
    @Override
    public void clearUserCache(Long userId) {
        log.info("Clearing cache for user with ID: {}", userId);
        Objects.requireNonNull(cacheManager.getCache("users")).evict(userId);
        log.info("Cache cleared for user with ID: {}", userId);
    }
    
    @Override
    public void clearAllUserCaches() {
        log.info("Clearing all user-related caches");
        
        // Clear users cache
        Objects.requireNonNull(cacheManager.getCache("users")).clear();
        
        // Clear activeUsers cache
        Objects.requireNonNull(cacheManager.getCache("activeUsers")).clear();
        
        // Clear userStats cache
        Objects.requireNonNull(cacheManager.getCache("userStats")).clear();
        
        log.info("All user-related caches cleared successfully");
    }
}
