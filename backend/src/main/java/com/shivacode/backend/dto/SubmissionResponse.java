
package com.shivacode.backend.dto;

import java.util.List;

public record SubmissionResponse(
        String verdict,
        int totalTestCases,
        int passedTestCases,
        String message,
        long executionTimeMs,
        List<TestCaseResult> testCaseResults
) {
    // Keeps existing five-argument constructor compatible.
    public SubmissionResponse(
            String verdict,
            int totalTestCases,
            int passedTestCases,
            String message,
            long executionTimeMs
    ) {
        this(
                verdict,
                totalTestCases,
                passedTestCases,
                message,
                executionTimeMs,
                List.of()
        );
    }

    public record TestCaseResult(
            int testCaseNumber,
            String verdict,
            String message
    ) {
    }
}


