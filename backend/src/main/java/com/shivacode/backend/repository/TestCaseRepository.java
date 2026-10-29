
package com.shivacode.backend.repository;

import com.shivacode.backend.entity.TestCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository
        extends JpaRepository<TestCaseEntity, Long> {

    List<TestCaseEntity> findByProblem_IdOrderByIdAsc(Long problemId);

}

