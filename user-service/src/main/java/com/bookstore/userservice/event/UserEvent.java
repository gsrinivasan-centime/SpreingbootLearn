package com.bookstore.userservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("active")
    private Boolean active;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
