package com.bookstore.userservice.controller;

import com.bookstore.userservice.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
@Tag(name = "Cache Management", description = "APIs for managing application caches")
public class CacheController {
    
    private final CacheService cacheService;
    
    @Operation(summary = "Clear user cache", description = "Clears the cache for a specific user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache cleared successfully")
    })
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> clearUserCache(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        log.info("Request to clear cache for user with ID: {}", userId);
        cacheService.clearUserCache(userId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Cache cleared successfully for user ID: " + userId));
    }
    
    @Operation(summary = "Clear all user caches", description = "Clears all caches related to users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All user caches cleared successfully")
    })
    @DeleteMapping("/users")
    public ResponseEntity<Map<String, String>> clearAllUserCaches() {
        log.info("Request to clear all user-related caches");
        cacheService.clearAllUserCaches();
        return ResponseEntity.ok(Collections.singletonMap("message", "All user caches cleared successfully"));
    }
}
