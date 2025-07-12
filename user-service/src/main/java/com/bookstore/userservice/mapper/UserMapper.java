package com.bookstore.userservice.mapper;

import com.bookstore.userservice.dto.CreateUserRequestDto;
import com.bookstore.userservice.dto.UpdateUserRequestDto;
import com.bookstore.userservice.dto.UserDto;
import com.bookstore.userservice.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserMapper {
    
    /**
     * Convert User entity to UserDto
     * @param user the user entity
     * @return the user DTO
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        
        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .active(user.getActive())
                .build();
    }
    
    /**
     * Convert CreateUserRequestDto to User entity
     * @param createUserRequest the create user request DTO
     * @return the user entity
     */
    public User toEntity(CreateUserRequestDto createUserRequest) {
        if (createUserRequest == null) {
            return null;
        }
        
        User user = new User();
        user.setFirstName(createUserRequest.getFirstName());
        user.setLastName(createUserRequest.getLastName());
        user.setEmail(createUserRequest.getEmail());
        user.setPhoneNumber(createUserRequest.getPhoneNumber());
        
        return user;
    }
    
    /**
     * Update existing User entity from UpdateUserRequestDto
     * @param updateUserRequest the update user request DTO
     * @param existingUser the existing user entity to update
     */
    public void updateEntityFromDto(UpdateUserRequestDto updateUserRequest, User existingUser) {
        if (updateUserRequest == null || existingUser == null) {
            return;
        }
        
        if (StringUtils.hasText(updateUserRequest.getFirstName())) {
            existingUser.setFirstName(updateUserRequest.getFirstName());
        }
        
        if (StringUtils.hasText(updateUserRequest.getLastName())) {
            existingUser.setLastName(updateUserRequest.getLastName());
        }
        
        if (StringUtils.hasText(updateUserRequest.getEmail())) {
            existingUser.setEmail(updateUserRequest.getEmail());
        }
        
        if (StringUtils.hasText(updateUserRequest.getPhoneNumber())) {
            existingUser.setPhoneNumber(updateUserRequest.getPhoneNumber());
        }
    }
}
