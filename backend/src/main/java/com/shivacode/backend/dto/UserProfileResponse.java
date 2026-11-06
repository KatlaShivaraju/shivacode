
package com.shivacode.backend.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        LocalDateTime memberSince,
        long totalSubmissions,
        long acceptedSubmissions,
        long solvedProblems,
        double acceptanceRate
) {
}

