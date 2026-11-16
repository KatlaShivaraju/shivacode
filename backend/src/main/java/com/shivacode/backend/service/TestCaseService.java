
package com.shivacode.backend.service;

import com.shivacode.backend.entity.ProblemEntity;
import com.shivacode.backend.entity.TestCaseEntity;
import com.shivacode.backend.repository.ProblemRepository;
import com.shivacode.backend.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            ProblemRepository problemRepository
    ) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
    }

    @Transactional
    public void seedTestCases() {

        // Create problems if they do not already exist.
        ProblemEntity twoSum = getOrCreateProblem(
                "Two Sum",
                "Easy",
                "Given an array of integers nums and an integer target, " +
                        "return the indices of the two numbers that add up to target.",
                "4\n2 7 11 15\n9",
                "0 1"
        );

        ProblemEntity reverseInteger = getOrCreateProblem(
                "Reverse Integer",
                "Medium",
                "Given a signed 32-bit integer x, reverse its digits. " +
                        "Return 0 if the reversed integer overflows the 32-bit signed range.",
                "123",
                "321"
        );

        ProblemEntity validParentheses = getOrCreateProblem(
                "Valid Parentheses",
                "Easy",
                "Given a string containing parentheses and brackets, " +
                        "determine whether the brackets are valid and correctly nested.",
                "()[]{}",
                "true"
        );

        // Seed test cases using the actual database entities.
        seedProblemCases(twoSum, List.of(
                new String[]{"4\n2 7 11 15\n9", "0 1"},
                new String[]{"3\n3 2 4\n6", "1 2"},
                new String[]{"2\n3 3\n6", "0 1"},
                new String[]{"4\n1 5 3 7\n10", "2 3"},
                new String[]{"4\n-1 -2 -3 -4\n-6", "1 3"},
                new String[]{"5\n1 2 3 4 5\n9", "3 4"}
        ));

        seedProblemCases(reverseInteger, List.of(
                new String[]{"123", "321"},
                new String[]{"-123", "-321"},
                new String[]{"120", "21"}
        ));

        seedProblemCases(validParentheses, List.of(
                new String[]{"()", "true"},
                new String[]{"()[]{}", "true"},
                new String[]{"(]", "false"}
        ));
    }

    private ProblemEntity getOrCreateProblem(
            String title,
            String difficulty,
            String description,
            String sampleInput,
            String sampleOutput
    ) {
        return problemRepository.findByTitle(title)
                .orElseGet(() -> problemRepository.save(
                        new ProblemEntity(
                                title,
                                difficulty,
                                description,
                                sampleInput,
                                sampleOutput
                        )
                ));
    }

    private void seedProblemCases(
            ProblemEntity problem,
            List<String[]> cases
    ) {
        // Avoid duplicate test cases on application restart.
        if (!testCaseRepository
                .findByProblem_IdOrderByIdAsc(problem.getId())
                .isEmpty()) {
            return;
        }

        for (String[] testCase : cases) {
            TestCaseEntity entity = new TestCaseEntity(
                    problem,
                    testCase[0],
                    testCase[1],
                    true
            );

            testCaseRepository.save(entity);
        }
    }
}


