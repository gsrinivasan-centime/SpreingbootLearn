package com.bookstore.bookservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${idempotency.ttl-minutes:60}")
    private int ttlMinutes;

    @Autowired
    public IdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<Object> getCachedResult(String idempotencyKey) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        logger.debug("Checking cached result for idempotency key: {}", idempotencyKey);
        
        try {
            Object cachedResult = redisTemplate.opsForValue().get(redisKey);
            return Optional.ofNullable(cachedResult);
        } catch (Exception e) {
            logger.error("Error retrieving cached result for idempotency key: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    public void cacheResult(String idempotencyKey, Object result) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        logger.debug("Caching result for idempotency key: {}", idempotencyKey);
        
        try {
            redisTemplate.opsForValue().set(redisKey, result, Duration.ofMinutes(ttlMinutes));
            logger.info("Successfully cached result for idempotency key: {}", idempotencyKey);
        } catch (Exception e) {
            logger.error("Error caching result for idempotency key: {}", idempotencyKey, e);
        }
    }

    public void invalidateCache(String idempotencyKey) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        logger.debug("Invalidating cache for idempotency key: {}", idempotencyKey);
        
        try {
            redisTemplate.delete(redisKey);
            logger.info("Successfully invalidated cache for idempotency key: {}", idempotencyKey);
        } catch (Exception e) {
            logger.error("Error invalidating cache for idempotency key: {}", idempotencyKey, e);
        }
    }

    public boolean exists(String idempotencyKey) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            logger.error("Error checking existence for idempotency key: {}", idempotencyKey, e);
            return false;
        }
    }
}
