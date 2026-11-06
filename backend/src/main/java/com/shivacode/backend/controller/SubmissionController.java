
package com.shivacode.backend.controller;

import com.shivacode.backend.dto.SubmissionResponse;
import com.shivacode.backend.entity.UserEntity;
import com.shivacode.backend.repository.SubmissionRepository;
import com.shivacode.backend.repository.UserRepository;
import com.shivacode.backend.service.SubmissionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public SubmissionController(
            SubmissionService submissionService,
            SubmissionRepository submissionRepository,
            UserRepository userRepository
    ) {
        this.submissionService = submissionService;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{problemId}/submit")
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable Long problemId,
            @RequestBody SubmitRequest request,
            Authentication authentication
    ) {
        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.language() == null || request.language().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UserEntity user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        SubmissionResponse response = submissionService.submit(
                problemId,
                request.code(),
                request.language(),
                user
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{problemId}/submissions")
    public ResponseEntity<List<SubmissionHistoryResponse>> getSubmissionHistory(
            @PathVariable Long problemId,
            Authentication authentication
    ) {
        UserEntity user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        List<SubmissionHistoryResponse> history =
                submissionRepository
                        .findByProblemIdAndUser_IdOrderBySubmittedAtDesc(
                                problemId,
                                user.getId()
                        )
                        .stream()
                        .map(submission -> new SubmissionHistoryResponse(
                                submission.getId(),
                                submission.getProblemId(),
                                submission.getLanguage(),
                                submission.getVerdict(),
                                submission.getExecutionTimeMs(),
                                submission.getSubmittedAt()
                        ))
                        .toList();

        return ResponseEntity.ok(history);
    }

    public record SubmitRequest(
            String code,
            String language
    ) {
    }

    public record SubmissionHistoryResponse(
            Long id,
            Long problemId,
            String language,
            String verdict,
            Long executionTimeMs,
            LocalDateTime submittedAt
    ) {
    }
}



