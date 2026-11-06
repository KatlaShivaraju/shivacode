
package com.shivacode.backend.service;

import com.shivacode.backend.dto.CodeExecutionRequest;
import com.shivacode.backend.dto.CodeExecutionResponse;
import com.shivacode.backend.dto.SubmissionResponse;
import com.shivacode.backend.entity.SubmissionEntity;
import com.shivacode.backend.entity.TestCaseEntity;
import com.shivacode.backend.entity.UserEntity;
import com.shivacode.backend.repository.SubmissionRepository;
import com.shivacode.backend.repository.TestCaseRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SubmissionService {

    private final SecureCodeExecutionService secureCodeExecutionService;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;

    public SubmissionService(
            SecureCodeExecutionService secureCodeExecutionService,
            TestCaseRepository testCaseRepository,
            SubmissionRepository submissionRepository
    ) {
        this.secureCodeExecutionService = secureCodeExecutionService;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
    }

    public SubmissionResponse submit(
            Long problemId,
            String code,
            String language,
            UserEntity user
    ) {

        List<TestCaseEntity> testCases =
                testCaseRepository.findByProblem_IdOrderByIdAsc(problemId);

        if (testCases.isEmpty()) {

            saveSubmission(
                    problemId, code, language,
                    "NO_TEST_CASES", 0L, user
            );

            return new SubmissionResponse(
                    "NO_TEST_CASES",
                    0,
                    0,
                    "No test cases are configured for this problem.",
                    0L,
                    List.of()
            );
        }

        List<String> inputs = testCases.stream()
                .map(TestCaseEntity::getInput)
                .toList();

        CodeExecutionRequest baseRequest =
                new CodeExecutionRequest(code, language, "");

        long submissionStart = System.nanoTime();

        List<CodeExecutionResponse> results;

        try {
            results = secureCodeExecutionService.executeBatch(
                    baseRequest,
                    inputs
            );
        } catch (Exception exception) {

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - submissionStart
            );

            saveSubmission(
                    problemId, code, language,
                    "JUDGE_ERROR", durationMs, user
            );

            return new SubmissionResponse(
                    "JUDGE_ERROR",
                    testCases.size(),
                    0,
                    "Judge execution failed. Please try again.",
                    durationMs,
                    List.of()
            );
        }

        long batchDurationMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - submissionStart
        );

        if (results == null || results.size() != testCases.size()) {

            saveSubmission(
                    problemId, code, language,
                    "JUDGE_ERROR", batchDurationMs, user
            );

            return new SubmissionResponse(
                    "JUDGE_ERROR",
                    testCases.size(),
                    0,
                    "Judge returned an incomplete test execution result.",
                    batchDurationMs,
                    List.of()
            );
        }

        int passed = 0;

        String overallVerdict = "ACCEPTED";
        String overallMessage = "All test cases passed!";

        List<SubmissionResponse.TestCaseResult> testCaseResults =
                new ArrayList<>();

        for (int i = 0; i < testCases.size(); i++) {

            TestCaseEntity testCase = testCases.get(i);
            CodeExecutionResponse result = results.get(i);

            String caseVerdict;
            String caseMessage;

            if (result == null) {

                caseVerdict = "JUDGE_ERROR";
                caseMessage = "Judge returned an empty execution result.";

            } else if ("COMPILATION_ERROR".equals(result.status())) {

                caseVerdict = "COMPILATION_ERROR";
                caseMessage = result.error();

            } else if ("TIME_LIMIT_EXCEEDED".equals(result.status())) {

                caseVerdict = "TIME_LIMIT_EXCEEDED";
                caseMessage = result.error();

            } else if ("MEMORY_LIMIT_EXCEEDED".equals(result.status())) {

                caseVerdict = "MEMORY_LIMIT_EXCEEDED";
                caseMessage = result.error();

            } else if ("RUNTIME_ERROR".equals(result.status())) {

                caseVerdict = "RUNTIME_ERROR";
                caseMessage = result.error();

            } else if ("ERROR".equals(result.status())) {

                caseVerdict = "JUDGE_ERROR";
                caseMessage = result.error();

            } else if (!"EXECUTED".equals(result.status())) {

                caseVerdict = "JUDGE_ERROR";
                caseMessage = "Unexpected execution status: "
                        + result.status();

            } else if (!normalize(result.output()).equals(
                    normalize(testCase.getExpectedOutput())
            )) {

                caseVerdict = "WRONG_ANSWER";
                caseMessage = "Output does not match expected output.";

            } else {

                caseVerdict = "PASSED";
                caseMessage = "Test case passed.";
            }

            if ("PASSED".equals(caseVerdict)) {

                passed++;

            } else if ("ACCEPTED".equals(overallVerdict)) {

                overallVerdict = caseVerdict;

                overallMessage = "Test case " + (i + 1)
                        + " failed: " + caseMessage;
            }

            testCaseResults.add(
                    new SubmissionResponse.TestCaseResult(
                            i + 1,
                            caseVerdict,
                            caseMessage
                    )
            );
        }

        saveSubmission(
                problemId,
                code,
                language,
                overallVerdict,
                batchDurationMs,
                user
        );

        return new SubmissionResponse(
                overallVerdict,
                testCases.size(),
                passed,
                overallMessage,
                batchDurationMs,
                testCaseResults
        );
    }

    private void saveSubmission(
            Long problemId,
            String code,
            String language,
            String verdict,
            long executionTimeMs,
            UserEntity user
    ) {

        SubmissionEntity submission = new SubmissionEntity();

        submission.setProblemId(problemId);
        submission.setUser(user);
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





