package com.foodfusion.auth.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String userId,
        String email,
        String displayName,
        List<String> roles
) {
}
