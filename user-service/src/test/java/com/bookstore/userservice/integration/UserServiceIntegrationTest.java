package com.bookstore.userservice.integration;

import com.bookstore.userservice.dto.CreateUserRequestDto;
import com.bookstore.userservice.dto.UpdateUserRequestDto;
import com.bookstore.userservice.dto.UserDto;
import com.bookstore.userservice.entity.User;
import com.bookstore.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class UserServiceIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clean up test data
        userRepository.deleteAll();
        
        // Create test user
        testUser = new User();
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@test.com");
        testUser.setPhoneNumber("+1234567890");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }
    
    @Test
    void createUser_ShouldCreateUserSuccessfully() throws Exception {
        // Given
        CreateUserRequestDto request = CreateUserRequestDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@test.com")
                .phoneNumber("+1987654321")
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("Jane")))
                .andExpect(jsonPath("$.lastName", is("Smith")))
                .andExpect(jsonPath("$.email", is("jane.smith@test.com")))
                .andExpect(jsonPath("$.phoneNumber", is("+1987654321")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }
    
    @Test
    void createUser_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        // Given
        CreateUserRequestDto request = CreateUserRequestDto.builder()
                .firstName("John")
                .lastName("Duplicate")
                .email(testUser.getEmail()) // Use existing email
                .phoneNumber("+1555555555")
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Duplicate Email")))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }
    
    @Test
    void createUser_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        // Given
        CreateUserRequestDto request = CreateUserRequestDto.builder()
                .firstName("") // Invalid: empty first name
                .lastName("Smith")
                .email("invalid-email") // Invalid email format
                .phoneNumber("123") // Invalid phone number
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors", notNullValue()));
    }
    
    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.firstName", is(testUser.getFirstName())))
                .andExpect(jsonPath("$.lastName", is(testUser.getLastName())))
                .andExpect(jsonPath("$.email", is(testUser.getEmail())))
                .andExpect(jsonPath("$.active", is(testUser.getActive())));
    }
    
    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("User Not Found")))
                .andExpect(jsonPath("$.message", containsString("User not found with ID: 99999")));
    }
    
    @Test
    void getAllUsers_ShouldReturnPageOfUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "firstName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.content[0].firstName", is(testUser.getFirstName())))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }
    
    @Test
    void getActiveUsers_ShouldReturnActiveUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$[0].active", is(true)));
    }
    
    @Test
    void searchUsersByName_ShouldReturnMatchingUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/search")
                        .param("namePattern", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName", is("John")));
    }
    
    @Test
    void updateUser_ShouldUpdateUserSuccessfully() throws Exception {
        // Given
        UpdateUserRequestDto request = UpdateUserRequestDto.builder()
                .firstName("Johnny")
                .lastName("Updated")
                .build();
        
        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.firstName", is("Johnny")))
                .andExpect(jsonPath("$.lastName", is("Updated")))
                .andExpect(jsonPath("$.email", is(testUser.getEmail()))); // Should remain unchanged
    }
    
    @Test
    void deactivateUser_ShouldDeactivateUserSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/v1/users/{id}/deactivate", testUser.getId()))
                .andExpect(status().isNoContent());
        
        // Verify user is deactivated
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assert !updatedUser.getActive();
    }
    
    @Test
    void activateUser_ShouldActivateUserSuccessfully() throws Exception {
        // Given - First deactivate the user
        testUser.setActive(false);
        userRepository.save(testUser);
        
        // When & Then
        mockMvc.perform(patch("/api/v1/users/{id}/activate", testUser.getId()))
                .andExpect(status().isNoContent());
        
        // Verify user is activated
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assert updatedUser.getActive();
    }
    
    @Test
    void deleteUser_ShouldDeleteUserSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", testUser.getId()))
                .andExpect(status().isNoContent());
        
        // Verify user is deleted
        assert userRepository.findById(testUser.getId()).isEmpty();
    }
    
    @Test
    void checkEmailExists_ShouldReturnTrue_WhenEmailExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/exists/email/{email}", testUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(true)));
    }
    
    @Test
    void checkEmailExists_ShouldReturnFalse_WhenEmailDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/exists/email/{email}", "nonexistent@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(false)));
    }
    
    @Test
    void getActiveUsersCount_ShouldReturnCorrectCount() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/count/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(1)));
    }
}
