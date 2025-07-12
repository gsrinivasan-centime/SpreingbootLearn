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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private KafkaProducerService kafkaProducerService;
    
    @Mock
    private IdempotencyService idempotencyService;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User sampleUser;
    private UserDto sampleUserDto;
    private CreateUserRequestDto createUserRequest;
    private UpdateUserRequestDto updateUserRequest;
    
    @BeforeEach
    void setUp() {
        // Setup sample user entity
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setEmail("john.doe@example.com");
        sampleUser.setPhoneNumber("+1234567890");
        sampleUser.setActive(true);
        sampleUser.setCreatedAt(LocalDateTime.now());
        sampleUser.setUpdatedAt(LocalDateTime.now());
        
        // Setup sample user DTO
        sampleUserDto = UserDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Setup create user request
        createUserRequest = CreateUserRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .build();
        
        // Setup update user request
        updateUserRequest = UpdateUserRequestDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();
    }
    
    @Test
    void createUser_ShouldCreateUserSuccessfully() {
        // Given
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())).thenReturn(false);
        when(userMapper.toEntity(createUserRequest)).thenReturn(sampleUser);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);
        
        // When
        UserDto result = userService.createUser(createUserRequest);
        
        // Then
        assertNotNull(result);
        assertEquals(sampleUserDto.getEmail(), result.getEmail());
        assertEquals(sampleUserDto.getFirstName(), result.getFirstName());
        
        verify(userRepository).existsByEmail(createUserRequest.getEmail());
        verify(userRepository).existsByPhoneNumber(createUserRequest.getPhoneNumber());
        verify(userRepository).save(any(User.class));
        verify(kafkaProducerService).publishUserCreatedEvent(sampleUser);
    }
    
    @Test
    void createUser_ShouldThrowDuplicateEmailException_WhenEmailExists() {
        // Given
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(true);
        
        // When & Then
        assertThrows(DuplicateEmailException.class, () -> {
            userService.createUser(createUserRequest);
        });
        
        verify(userRepository).existsByEmail(createUserRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void createUser_ShouldThrowDuplicatePhoneNumberException_WhenPhoneNumberExists() {
        // Given
        when(userRepository.existsByEmail(createUserRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())).thenReturn(true);
        
        // When & Then
        assertThrows(DuplicatePhoneNumberException.class, () -> {
            userService.createUser(createUserRequest);
        });
        
        verify(userRepository).existsByEmail(createUserRequest.getEmail());
        verify(userRepository).existsByPhoneNumber(createUserRequest.getPhoneNumber());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);
        
        // When
        UserDto result = userService.getUserById(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(sampleUserDto.getId(), result.getId());
        assertEquals(sampleUserDto.getEmail(), result.getEmail());
        
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(sampleUser);
    }
    
    @Test
    void getUserById_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(userId);
        });
        
        verify(userRepository).findById(userId);
        verify(userMapper, never()).toDto(any(User.class));
    }
    
    @Test
    void getAllUsers_ShouldReturnPageOfUsers() {
        // Given
        Pageable pageable = mock(Pageable.class);
        List<User> users = Arrays.asList(sampleUser);
        Page<User> userPage = new PageImpl<>(users);
        
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);
        
        // When
        Page<UserDto> result = userService.getAllUsers(pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(sampleUserDto.getEmail(), result.getContent().get(0).getEmail());
        
        verify(userRepository).findAll(pageable);
    }
    
    @Test
    void updateUser_ShouldUpdateUserSuccessfully() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleUserDto);
        
        // When
        UserDto result = userService.updateUser(userId, updateUserRequest);
        
        // Then
        assertNotNull(result);
        
        verify(userRepository).findById(userId);
        verify(userMapper).updateEntityFromDto(updateUserRequest, sampleUser);
        verify(userRepository).save(sampleUser);
        verify(kafkaProducerService).publishUserUpdatedEvent(sampleUser);
    }
    
    @Test
    void updateUser_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            userService.updateUser(userId, updateUserRequest);
        });
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void deactivateUser_ShouldDeactivateUserSuccessfully() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        
        // When
        userService.deactivateUser(userId);
        
        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(sampleUser);
        verify(kafkaProducerService).publishUserDeactivatedEvent(sampleUser);
        assertFalse(sampleUser.getActive());
    }
    
    @Test
    void activateUser_ShouldActivateUserSuccessfully() {
        // Given
        Long userId = 1L;
        sampleUser.setActive(false); // Start with inactive user
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        
        // When
        userService.activateUser(userId);
        
        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(sampleUser);
        verify(kafkaProducerService).publishUserActivatedEvent(sampleUser);
        assertTrue(sampleUser.getActive());
    }
    
    @Test
    void deleteUser_ShouldDeleteUserSuccessfully() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        
        // When
        userService.deleteUser(userId);
        
        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).delete(sampleUser);
        verify(kafkaProducerService).publishUserDeletedEvent(sampleUser);
    }
    
    @Test
    void getActiveUsersCount_ShouldReturnCount() {
        // Given
        long expectedCount = 5L;
        when(userRepository.countActiveUsers()).thenReturn(expectedCount);
        
        // When
        long result = userService.getActiveUsersCount();
        
        // Then
        assertEquals(expectedCount, result);
        verify(userRepository).countActiveUsers();
    }
}
