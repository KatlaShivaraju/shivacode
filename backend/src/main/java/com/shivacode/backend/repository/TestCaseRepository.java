
package com.shivacode.backend.repository;

import com.shivacode.backend.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository
        extends JpaRepository<TestCaseEntity, Long> {

    // Fetch all test cases for submission evaluation.
    List<TestCaseEntity> findByProblem_IdOrderByIdAsc(Long problemId);

    // Fetch only public test cases for the frontend.
    List<TestCaseEntity> findByProblem_IdAndHiddenFalseOrderByIdAsc(
            Long problemId
    );
}


