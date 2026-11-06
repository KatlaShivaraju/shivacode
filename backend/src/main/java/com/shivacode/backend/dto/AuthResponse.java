
package com.shivacode.backend.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String email
) {
}

