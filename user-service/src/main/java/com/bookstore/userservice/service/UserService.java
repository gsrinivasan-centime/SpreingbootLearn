package com.bookstore.userservice.service;

import com.bookstore.userservice.dto.CreateUserRequestDto;
import com.bookstore.userservice.dto.UpdateUserRequestDto;
import com.bookstore.userservice.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    
    /**
     * Create a new user
     * @param createUserRequest the user creation request
     * @return the created user DTO
     */
    UserDto createUser(CreateUserRequestDto createUserRequest);
    
    /**
     * Get user by ID
     * @param id the user ID
     * @return the user DTO
     */
    UserDto getUserById(Long id);
    
    /**
     * Get user by email
     * @param email the email address
     * @return the user DTO
     */
    UserDto getUserByEmail(String email);
    
    /**
     * Get all users with pagination
     * @param pageable the pagination information
     * @return page of user DTOs
     */
    Page<UserDto> getAllUsers(Pageable pageable);
    
    /**
     * Get all active users
     * @return list of active user DTOs
     */
    List<UserDto> getActiveUsers();
    
    /**
     * Search users by name pattern
     * @param namePattern the name pattern to search for
     * @return list of matching user DTOs
     */
    List<UserDto> searchUsersByName(String namePattern);
    
    /**
     * Update user
     * @param id the user ID
     * @param updateUserRequest the user update request
     * @return the updated user DTO
     */
    UserDto updateUser(Long id, UpdateUserRequestDto updateUserRequest);
    
    /**
     * Deactivate user (soft delete)
     * @param id the user ID
     */
    void deactivateUser(Long id);
    
    /**
     * Activate user
     * @param id the user ID
     */
    void activateUser(Long id);
    
    /**
     * Delete user (hard delete)
     * @param id the user ID
     */
    void deleteUser(Long id);
    
    /**
     * Check if user exists by email
     * @param email the email address
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if user exists by phone number
     * @param phoneNumber the phone number
     * @return true if user exists, false otherwise
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * Get total count of active users
     * @return the count of active users
     */
    long getActiveUsersCount();
}
