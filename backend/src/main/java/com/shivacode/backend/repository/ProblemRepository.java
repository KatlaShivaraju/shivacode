
package com.shivacode.backend.repository;

import com.shivacode.backend.entity.ProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProblemRepository
        extends JpaRepository<ProblemEntity, Long> {

    Page<ProblemEntity> findByDifficultyIgnoreCase(
            String difficulty,
            Pageable pageable
    );
}

