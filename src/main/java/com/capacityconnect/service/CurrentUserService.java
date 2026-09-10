package com.capacityconnect.service;

import com.capacityconnect.entity.User;
import com.capacityconnect.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();

        String keycloakId = jwt.getSubject();

        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalStateException("Authenticated Keycloak ID not found");
        }

        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> createNewUser(jwt, keycloakId));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private User createNewUser(Jwt jwt, String keycloakId) {
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Authenticated email not found");
        }

        if (username == null || username.isBlank()) {
            username = email;
        }

        if (firstName == null || firstName.isBlank()) {
            firstName = username;
        }

        if (lastName == null) {
            lastName = "";
        }

        User user = User.builder()
                .username(username)
                .keycloakId(keycloakId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(User.Role.TRAINEE)
                .roles(new HashSet<>())
                .department(null)
                .status(User.Status.ACTIVE)
                .build();

        user.getRoles().add(User.Role.TRAINEE);

        return userRepository.save(user);
    }
}
