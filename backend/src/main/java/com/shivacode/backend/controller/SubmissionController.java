
package com.shivacode.backend.controller;

import com.shivacode.backend.dto.SubmissionResponse;
import com.shivacode.backend.entity.SubmissionEntity;
import com.shivacode.backend.repository.SubmissionRepository;
import com.shivacode.backend.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;

    public SubmissionController(
            SubmissionService submissionService,
            SubmissionRepository submissionRepository
    ) {
        this.submissionService = submissionService;
        this.submissionRepository = submissionRepository;
    }

    @PostMapping("/{problemId}/submit")
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable Long problemId,
            @RequestBody SubmitRequest request
    ) {
        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.language() == null
                || request.language().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        SubmissionResponse response = submissionService.submit(
                problemId,
                request.code(),
                request.language()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{problemId}/submissions")
    public ResponseEntity<List<SubmissionHistoryResponse>> getSubmissionHistory(
            @PathVariable Long problemId
    ) {
        List<SubmissionHistoryResponse> history =
                submissionRepository
                        .findByProblemIdOrderBySubmittedAtDesc(problemId)
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


