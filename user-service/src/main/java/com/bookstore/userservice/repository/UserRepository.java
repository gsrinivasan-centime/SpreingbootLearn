package com.bookstore.userservice.repository;

import com.bookstore.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email address
     * @param email the email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by phone number (encrypted)
     * @param phoneNumber the encrypted phone number
     * @return Optional containing the user if found
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    /**
     * Find all active users
     * @return List of active users
     */
    List<User> findByActiveTrue();
    
    /**
     * Find users by first name containing the given string (case-insensitive)
     * @param firstName the first name pattern
     * @return List of matching users
     */
    List<User> findByFirstNameContainingIgnoreCase(String firstName);
    
    /**
     * Find users by last name containing the given string (case-insensitive)
     * @param lastName the last name pattern
     * @return List of matching users
     */
    List<User> findByLastNameContainingIgnoreCase(String lastName);
    
    /**
     * Check if a user exists with the given email
     * @param email the email address
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if a user exists with the given phone number
     * @param phoneNumber the encrypted phone number
     * @return true if user exists, false otherwise
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * Count active users
     * @return the number of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();
    
    /**
     * Find users by name pattern (first name or last name)
     * @param namePattern the name pattern to search for
     * @return List of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :namePattern, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<User> findByNamePattern(@Param("namePattern") String namePattern);
}
