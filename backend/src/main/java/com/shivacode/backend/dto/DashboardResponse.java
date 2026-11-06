
package com.shivacode.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        List<DailyActivity> dailyActivity,
        List<DifficultyStats> difficultyStats,
        List<RecentSubmission> recentSubmissions
) {

    public record DailyActivity(
            LocalDate date,
            long submissions
    ) {}

    public record DifficultyStats(
            String difficulty,
            long solved
    ) {}

    public record RecentSubmission(
            Long id,
            Long problemId,
            String problemTitle,
            String language,
            String verdict,
            Long executionTimeMs,
            LocalDateTime submittedAt
    ) {}
}

