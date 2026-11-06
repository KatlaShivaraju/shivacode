
package com.shivacode.backend.service;

import com.shivacode.backend.dto.UserProfileResponse;
import com.shivacode.backend.entity.UserEntity;
import com.shivacode.backend.repository.SubmissionRepository;
import com.shivacode.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    public UserProfileService(
            UserRepository userRepository,
            SubmissionRepository submissionRepository
    ) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    public UserProfileResponse getProfile(String email) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        Long userId = user.getId();

        long totalSubmissions =
                submissionRepository.countByUser_Id(userId);

        long acceptedSubmissions =
                submissionRepository.countByUser_IdAndVerdict(
                        userId,
                        "ACCEPTED"
                );

        long solvedProblems =
                submissionRepository.countDistinctSolvedProblems(userId);

        double acceptanceRate = totalSubmissions == 0
                ? 0.0
                : Math.round(
                (acceptedSubmissions * 10000.0) / totalSubmissions
        ) / 100.0;

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                totalSubmissions,
                acceptedSubmissions,
                solvedProblems,
                acceptanceRate
        );
    }
}

