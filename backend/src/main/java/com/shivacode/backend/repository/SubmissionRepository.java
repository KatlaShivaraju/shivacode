
package com.shivacode.backend.repository;

import com.shivacode.backend.entity.SubmissionEntity;
import com.shivacode.backend.entity.ProblemEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubmissionRepository
        extends JpaRepository<SubmissionEntity, Long> {

    // Existing submission history
    List<SubmissionEntity> findByProblemIdAndUser_IdOrderBySubmittedAtDesc(
            Long problemId,
            Long userId
    );

    // Total submissions by a user
    long countByUser_Id(Long userId);

    // Accepted submissions by a user
    long countByUser_IdAndVerdict(Long userId, String verdict);

    // Distinct problems solved by a user
    @Query("""
            SELECT COUNT(DISTINCT s.problemId)
            FROM SubmissionEntity s
            WHERE s.user.id = :userId
            AND s.verdict = 'ACCEPTED'
            """)
    long countDistinctSolvedProblems(@Param("userId") Long userId);

    // Latest submissions by a user
    List<SubmissionEntity> findByUser_IdOrderBySubmittedAtDesc(
            Long userId,
            Pageable pageable
    );

    // Daily submission activity
    @Query("""
            SELECT FUNCTION('DATE', s.submittedAt), COUNT(s)
            FROM SubmissionEntity s
            WHERE s.user.id = :userId
            AND s.submittedAt >= :startDate
            GROUP BY FUNCTION('DATE', s.submittedAt)
            ORDER BY FUNCTION('DATE', s.submittedAt)
            """)
    List<Object[]> getDailySubmissionActivity(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    // Unique accepted problems grouped by difficulty
    @Query("""
            SELECT p.difficulty, COUNT(DISTINCT s.problemId)
            FROM SubmissionEntity s
            JOIN ProblemEntity p ON p.id = s.problemId
            WHERE s.user.id = :userId
            AND s.verdict = 'ACCEPTED'
            GROUP BY p.difficulty
            """)
    List<Object[]> getSolvedProblemsByDifficulty(
            @Param("userId") Long userId
    );
}




