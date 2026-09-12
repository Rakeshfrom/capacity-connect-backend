package com.capacityconnect.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String idToken,
        Long expiresIn,
        Long refreshExpiresIn,
        String tokenType,
        String sessionState
) {}
