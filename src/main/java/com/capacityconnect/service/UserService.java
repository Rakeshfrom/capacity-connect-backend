package com.capacityconnect.service;

import com.capacityconnect.dto.UserRequest;
import com.capacityconnect.dto.UserResponse;
import com.capacityconnect.entity.User;
import com.capacityconnect.exception.DuplicateResourceException;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.UserRepository;
import com.capacityconnect.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = findUser(id);
        return toResponse(user);
    }

    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .department(request.getDepartment())
                .status(request.getStatus())
                .build();

        User savedUser = userRepository.save(user);

        auditLogService.create(
                null,
                "System",
                "User created",
                "User Management",
                savedUser.getFirstName() + " " + savedUser.getLastName(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = findUser(id);

        userRepository.findByUsername(request.getUsername())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Username already exists: " + request.getUsername());
                });

        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Email already exists: " + request.getEmail());
                });

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setDepartment(request.getDepartment());
        user.setStatus(request.getStatus());

        User updatedUser = userRepository.save(user);

        auditLogService.create(
                null,
                "System",
                "User updated",
                "User Management",
                updatedUser.getFirstName() + " " + updatedUser.getLastName(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        User user = findUser(id);
        String target = user.getFirstName() + " " + user.getLastName();

        userRepository.delete(user);

        auditLogService.create(
                null,
                "System",
                "User deleted",
                "User Management",
                target,
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .department(user.getDepartment())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
