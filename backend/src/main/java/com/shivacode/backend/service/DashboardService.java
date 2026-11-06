
package com.shivacode.backend.service;

import com.shivacode.backend.dto.DashboardResponse;
import com.shivacode.backend.entity.ProblemEntity;
import com.shivacode.backend.entity.SubmissionEntity;
import com.shivacode.backend.repository.ProblemRepository;
import com.shivacode.backend.repository.SubmissionRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;

    public DashboardService(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {

        // 1. Daily activity for the last 30 days
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);

        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<Object[]> activityRows =
                submissionRepository.getDailySubmissionActivity(
                        userId,
                        startDateTime
                );

        Map<LocalDate, Long> activityMap = activityRows.stream()
                .collect(Collectors.toMap(
                        row -> {
                            Object dateValue = row[0];

                            if (dateValue instanceof java.sql.Date sqlDate) {
                                return sqlDate.toLocalDate();
                            }

                            return LocalDate.parse(dateValue.toString());
                        },
                        row -> ((Number) row[1]).longValue()
                ));

        List<DashboardResponse.DailyActivity> dailyActivity =
                new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);

            dailyActivity.add(
                    new DashboardResponse.DailyActivity(
                            date,
                            activityMap.getOrDefault(date, 0L)
                    )
            );
        }

        // 2. Solved problems grouped by difficulty
        List<Object[]> difficultyRows =
                submissionRepository.getSolvedProblemsByDifficulty(userId);

        List<DashboardResponse.DifficultyStats> difficultyStats =
                difficultyRows.stream()
                        .map(row -> new DashboardResponse.DifficultyStats(
                                (String) row[0],
                                ((Number) row[1]).longValue()
                        ))
                        .toList();

        // 3. Latest 10 submissions
        List<SubmissionEntity> recentEntities =
                submissionRepository.findByUser_IdOrderBySubmittedAtDesc(
                        userId,
                        PageRequest.of(0, 10)
                );

        // Fetch problem titles
        List<Long> problemIds = recentEntities.stream()
                .map(SubmissionEntity::getProblemId)
                .distinct()
                .toList();

        Map<Long, ProblemEntity> problemMap =
                problemRepository.findAllById(problemIds)
                        .stream()
                        .collect(Collectors.toMap(
                                ProblemEntity::getId,
                                Function.identity()
                        ));

        List<DashboardResponse.RecentSubmission> recentSubmissions =
                recentEntities.stream()
                        .map(submission -> {
                            ProblemEntity problem =
                                    problemMap.get(submission.getProblemId());

                            return new DashboardResponse.RecentSubmission(
                                    submission.getId(),
                                    submission.getProblemId(),
                                    problem != null
                                            ? problem.getTitle()
                                            : "Unknown Problem",
                                    submission.getLanguage(),
                                    submission.getVerdict(),
                                    submission.getExecutionTimeMs(),
                                    submission.getSubmittedAt()
                            );
                        })
                        .toList();

        return new DashboardResponse(
                dailyActivity,
                difficultyStats,
                recentSubmissions
        );
    }
}

