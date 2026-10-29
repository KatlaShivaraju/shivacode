
package com.shivacode.backend.service;

import com.shivacode.backend.dto.CodeExecutionRequest;
import com.shivacode.backend.dto.CodeExecutionResponse;
import com.shivacode.backend.dto.SubmissionResponse;
import com.shivacode.backend.entity.SubmissionEntity;
import com.shivacode.backend.entity.TestCaseEntity;
import com.shivacode.backend.repository.SubmissionRepository;
import com.shivacode.backend.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SubmissionService {

    private final CodeExecutionService codeExecutionService;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;

    public SubmissionService(
            CodeExecutionService codeExecutionService,
            TestCaseRepository testCaseRepository,
            SubmissionRepository submissionRepository
    ) {
        this.codeExecutionService = codeExecutionService;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
    }

    public SubmissionResponse submit(
            Long problemId,
            String code,
            String language
    ) {
        List<TestCaseEntity> testCases =
                testCaseRepository.findByProblem_IdOrderByIdAsc(problemId);

        if (testCases.isEmpty()) {
            saveSubmission(
                    problemId, code, language,
                    "NO_TEST_CASES", 0L
            );

            return new SubmissionResponse(
                    "NO_TEST_CASES", 0, 0,
                    "No test cases are configured for this problem.", 0
            );
        }

        List<String> inputs = testCases.stream()
                .map(TestCaseEntity::getInput)
                .toList();

        CodeExecutionRequest baseRequest =
                new CodeExecutionRequest(code, language, "");

        long submissionStart = System.nanoTime();

        List<CodeExecutionResponse> results =
                codeExecutionService.executeBatch(baseRequest, inputs);

        long batchDurationMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - submissionStart
        );

        System.out.println(
                "ShivaCode batch execution time: "
                        + batchDurationMs + " ms"
        );

        int passed = 0;
        String verdict = "ACCEPTED";
        String message = "All test cases passed!";

        for (int i = 0; i < testCases.size(); i++) {
            TestCaseEntity testCase = testCases.get(i);
            CodeExecutionResponse result = results.get(i);

            System.out.println("Test case: " + (i + 1));
            System.out.println("Input: " + testCase.getInput());
            System.out.println(
                    "Expected: [" + testCase.getExpectedOutput() + "]"
            );
            System.out.println("Actual: [" + result.output() + "]");
            System.out.println("Status: " + result.status());

            if ("COMPILATION_ERROR".equals(result.status())) {
                verdict = "COMPILATION_ERROR";
                message = result.error();
                break;
            }

            if ("TIME_LIMIT_EXCEEDED".equals(result.status())) {
                verdict = "TIME_LIMIT_EXCEEDED";
                message = result.error();
                break;
            }

            if ("RUNTIME_ERROR".equals(result.status())
                    || "ERROR".equals(result.status())) {
                verdict = "RUNTIME_ERROR";
                message = result.error();
                break;
            }

            if (!"EXECUTED".equals(result.status())) {
                verdict = "JUDGE_ERROR";
                message = "Unexpected execution status: "
                        + result.status();
                break;
            }

            if (!normalize(result.output())
                    .equals(normalize(testCase.getExpectedOutput()))) {
                verdict = "WRONG_ANSWER";
                message = "Test case " + (i + 1) + " failed.";
                break;
            }

            passed++;
        }

        saveSubmission(
                problemId, code, language, verdict, batchDurationMs
        );

        return new SubmissionResponse(
                verdict,
                testCases.size(),
                passed,
                message,
                batchDurationMs
        );
    }

    private void saveSubmission(
            Long problemId,
            String code,
            String language,
            String verdict,
            long executionTimeMs
    ) {
        SubmissionEntity submission = new SubmissionEntity();

        submission.setProblemId(problemId);
        submission.setSourceCode(code);
        submission.setLanguage(language);
        submission.setVerdict(verdict);
        submission.setExecutionTimeMs(executionTimeMs);

        submissionRepository.save(submission);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}

