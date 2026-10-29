
package com.shivacode.backend.dto;

public record SubmissionResponse(
        String verdict,
        int totalTestCases,
        int passedTestCases,
        String message,
        long executionTimeMs
) {
}

