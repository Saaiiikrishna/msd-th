package com.mysillydreams.userservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Event published when a new user is created
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCreatedEvent extends UserEvent {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String gender;
    private boolean active;
    private Set<String> roles;

    public UserCreatedEvent(String userReferenceId, UUID userId, String email, String firstName, 
                           String lastName, String phone, String gender, Set<String> roles) {
        super("USER_CREATED", userReferenceId, userId);
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.gender = gender;
        this.active = true;
        this.roles = roles;
    }
}
