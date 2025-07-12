package com.bookstore.userservice.service.impl;

import com.bookstore.userservice.dto.CreateUserRequestDto;
import com.bookstore.userservice.dto.UpdateUserRequestDto;
import com.bookstore.userservice.dto.UserDto;
import com.bookstore.userservice.entity.User;
import com.bookstore.userservice.exception.DuplicateEmailException;
import com.bookstore.userservice.exception.DuplicatePhoneNumberException;
import com.bookstore.userservice.exception.UserNotFoundException;
import com.bookstore.userservice.mapper.UserMapper;
import com.bookstore.userservice.repository.UserRepository;
import com.bookstore.userservice.service.IdempotencyService;
import com.bookstore.userservice.service.KafkaProducerService;
import com.bookstore.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KafkaProducerService kafkaProducerService;
    private final IdempotencyService idempotencyService;
    
    @Override
    public UserDto createUser(CreateUserRequestDto createUserRequest) {
        log.info("Creating new user with email: {}", createUserRequest.getEmail());
        
        // Check for duplicate email
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            log.error("User with email {} already exists", createUserRequest.getEmail());
            throw new DuplicateEmailException("User with email " + createUserRequest.getEmail() + " already exists");
        }
        
        // Check for duplicate phone number
        if (userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())) {
            log.error("User with phone number already exists");
            throw new DuplicatePhoneNumberException("User with this phone number already exists");
        }
        
        User user = userMapper.toEntity(createUserRequest);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        // Publish user created event
        kafkaProducerService.publishUserCreatedEvent(savedUser);
        
        return userMapper.toDto(savedUser);
    }
    
    @Override
    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return userMapper.toDto(user);
    }
    
    @Override
    @Cacheable(value = "users", key = "#email")
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        log.info("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return userMapper.toDto(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toDto);
    }
    
    @Override
    @Cacheable(value = "activeUsers")
    @Transactional(readOnly = true)
    public List<UserDto> getActiveUsers() {
        log.info("Fetching all active users");
        List<User> activeUsers = userRepository.findByActiveTrue();
        return activeUsers.stream()
                .map(userMapper::toDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> searchUsersByName(String namePattern) {
        log.info("Searching users by name pattern: {}", namePattern);
        List<User> users = userRepository.findByNamePattern(namePattern);
        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }
    
    @Override
    @CacheEvict(value = {"users", "activeUsers"}, allEntries = true)
    public UserDto updateUser(Long id, UpdateUserRequestDto updateUserRequest) {
        log.info("Updating user with ID: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        
        // Check for duplicate email if email is being updated
        if (StringUtils.hasText(updateUserRequest.getEmail()) && 
            !updateUserRequest.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(updateUserRequest.getEmail())) {
                throw new DuplicateEmailException("User with email " + updateUserRequest.getEmail() + " already exists");
            }
        }
        
        // Check for duplicate phone number if phone number is being updated
        if (StringUtils.hasText(updateUserRequest.getPhoneNumber()) &&
            !updateUserRequest.getPhoneNumber().equals(existingUser.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(updateUserRequest.getPhoneNumber())) {
                throw new DuplicatePhoneNumberException("User with this phone number already exists");
            }
        }
        
        // Update fields
        userMapper.updateEntityFromDto(updateUserRequest, existingUser);
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(existingUser);
        log.info("User updated successfully with ID: {}", updatedUser.getId());
        
        // Publish user updated event
        kafkaProducerService.publishUserUpdatedEvent(updatedUser);
        
        return userMapper.toDto(updatedUser);
    }
    
    @Override
    @CacheEvict(value = {"users", "activeUsers"}, allEntries = true)
    public void deactivateUser(Long id) {
        log.info("Deactivating user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User deactivated successfully with ID: {}", id);
        
        // Publish user deactivated event
        kafkaProducerService.publishUserDeactivatedEvent(user);
    }
    
    @Override
    @CacheEvict(value = {"users", "activeUsers"}, allEntries = true)
    public void activateUser(Long id) {
        log.info("Activating user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User activated successfully with ID: {}", id);
        
        // Publish user activated event
        kafkaProducerService.publishUserActivatedEvent(user);
    }
    
    @Override
    @CacheEvict(value = {"users", "activeUsers"}, allEntries = true)
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        
        userRepository.delete(user);
        log.info("User deleted successfully with ID: {}", id);
        
        // Publish user deleted event
        kafkaProducerService.publishUserDeletedEvent(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }
    
    @Override
    @Cacheable(value = "userStats", key = "'activeCount'")
    @Transactional(readOnly = true)
    public long getActiveUsersCount() {
        return userRepository.countActiveUsers();
    }
}
