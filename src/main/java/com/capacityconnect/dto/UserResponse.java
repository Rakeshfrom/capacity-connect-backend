package com.capacityconnect.dto;

import com.capacityconnect.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private User.Role role;
    private Set<User.Role> roles;
    private String department;
    private String phoneNumber;
    private String qualifications;
    private String skills;
    private Integer experienceYears;
    private String interests;
    private String profilePicUrl;
    private boolean profileCompleted;
    private User.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
