
package com.shivacode.backend.controller;

import com.shivacode.backend.Problem;
import com.shivacode.backend.entity.TestCaseEntity;
import com.shivacode.backend.repository.TestCaseRepository;
import com.shivacode.backend.service.ProblemService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final TestCaseRepository testCaseRepository;

    public ProblemController(
            ProblemService problemService,
            TestCaseRepository testCaseRepository
    ) {
        this.problemService = problemService;
        this.testCaseRepository = testCaseRepository;
    }

    // Get a problem by ID.
    @GetMapping("/{id}")
    public Problem getProblemById(@PathVariable Long id) {
        return problemService.getProblemById(id);
    }

    // Get all problems with optional difficulty filtering.
    @GetMapping
    public Page<Problem> getAllProblems(
            @RequestParam(required = false) String difficulty,
            Pageable pageable
    ) {
        if (difficulty != null && !difficulty.isBlank()) {
            return problemService.getProblemsByDifficulty(
                    difficulty, pageable
            );
        }

        return problemService.getAllProblems(pageable);
    }

    // Get only public test cases for a problem.
    @GetMapping("/{id}/test-cases")
    public List<PublicTestCaseResponse> getPublicTestCases(
            @PathVariable Long id
    ) {
        return testCaseRepository
                .findByProblem_IdAndHiddenFalseOrderByIdAsc(id)
                .stream()
                .map(testCase -> new PublicTestCaseResponse(
                        testCase.getId(),
                        testCase.getInput(),
                        testCase.getExpectedOutput()
                ))
                .toList();
    }

    // Response DTO: never expose hidden test cases.
    public record PublicTestCaseResponse(
            Long id,
            String input,
            String expectedOutput
    ) {}
}


