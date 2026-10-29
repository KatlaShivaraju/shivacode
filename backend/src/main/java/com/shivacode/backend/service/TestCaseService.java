
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

        // Problem ID 1: Two Sum
        seedProblemCases(
                1L,
                // Problem ID 1: Two Sum
                        List.of(
                                new String[]{"4\n2 7 11 15\n9", "0 1"},
                                new String[]{"3\n3 2 4\n6", "1 2"},
                                new String[]{"2\n3 3\n6", "0 1"},
                                new String[]{"4\n1 5 3 7\n10", "2 3"},
                                new String[]{"4\n-1 -2 -3 -4\n-6", "1 3"},
                                new String[]{"5\n1 2 3 4 5\n9", "3 4"}
                        )
                );

        // Problem ID 2: Reverse Integer
        seedProblemCases(
                2L,
                List.of(
                        new String[]{"123", "321"},
                        new String[]{"-123", "-321"},
                        new String[]{"120", "21"}
                )
        );

        // Problem ID 3: Valid Parentheses
        seedProblemCases(
                3L,
                List.of(
                        new String[]{"()", "true"},
                        new String[]{"()[]{}", "true"},
                        new String[]{"(]", "false"}
                )
        );
    }

    private void seedProblemCases(
            Long problemId,
            List<String[]> cases
    ) {
        // Avoid inserting duplicate test cases on restart.
        if (!testCaseRepository
                .findByProblem_IdOrderByIdAsc(problemId)
                .isEmpty()) {
            return;
        }

        ProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Problem not found: " + problemId
                        )
                );

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


