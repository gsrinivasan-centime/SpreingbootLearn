package com.bookstore.userservice.controller;

import com.bookstore.userservice.dto.CreateUserRequestDto;
import com.bookstore.userservice.dto.UpdateUserRequestDto;
import com.bookstore.userservice.dto.UserDto;
import com.bookstore.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users in the bookstore")
public class UserController {
    
    private final UserService userService;
    
    @Operation(summary = "Create a new user", description = "Creates a new user with the provided information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "User with email or phone number already exists")
    })
    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequestDto createUserRequest) {
        log.info("Creating new user with email: {}", createUserRequest.getEmail());
        UserDto createdUser = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        log.info("Fetching user by ID: {}", id);
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @Operation(summary = "Get user by email", description = "Retrieves a user by their email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(
            @Parameter(description = "User email", required = true) @PathVariable String email) {
        log.info("Fetching user by email: {}", email);
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    
    @Operation(summary = "Get all users", description = "Retrieves all users with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
    
    @Operation(summary = "Get active users", description = "Retrieves all active users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active users retrieved successfully")
    })
    @GetMapping("/active")
    public ResponseEntity<List<UserDto>> getActiveUsers() {
        log.info("Fetching all active users");
        List<UserDto> activeUsers = userService.getActiveUsers();
        return ResponseEntity.ok(activeUsers);
    }
    
    @Operation(summary = "Search users by name", description = "Searches for users by first name or last name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users found")
    })
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsersByName(
            @Parameter(description = "Name pattern to search for", required = true) 
            @RequestParam String namePattern) {
        log.info("Searching users by name pattern: {}", namePattern);
        List<UserDto> users = userService.searchUsersByName(namePattern);
        return ResponseEntity.ok(users);
    }
    
    @Operation(summary = "Update user", description = "Updates an existing user's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email or phone number already exists")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto updateUserRequest) {
        log.info("Updating user with ID: {}", id);
        UserDto updatedUser = userService.updateUser(id, updateUserRequest);
        return ResponseEntity.ok(updatedUser);
    }
    
    @Operation(summary = "Deactivate user", description = "Deactivates a user (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        log.info("Deactivating user with ID: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Activate user", description = "Activates a previously deactivated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User activated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        log.info("Activating user with ID: {}", id);
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Delete user", description = "Permanently deletes a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Check if email exists", description = "Checks if a user with the given email exists")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed")
    })
    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(
            @Parameter(description = "Email to check", required = true) @PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
    
    @Operation(summary = "Check if phone number exists", description = "Checks if a user with the given phone number exists")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed")
    })
    @GetMapping("/exists/phone/{phoneNumber}")
    public ResponseEntity<Map<String, Boolean>> checkPhoneNumberExists(
            @Parameter(description = "Phone number to check", required = true) @PathVariable String phoneNumber) {
        boolean exists = userService.existsByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
    
    @Operation(summary = "Get active users count", description = "Returns the total count of active users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    @GetMapping("/count/active")
    public ResponseEntity<Map<String, Long>> getActiveUsersCount() {
        long count = userService.getActiveUsersCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
