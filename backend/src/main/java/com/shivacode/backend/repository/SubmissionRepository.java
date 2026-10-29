package com.shivacode.backend.repository;
import com.shivacode.backend.entity.SubmissionEntity;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository
        extends JpaRepository<SubmissionEntity, Long> {

    List<SubmissionEntity> findByProblemIdOrderBySubmittedAtDesc(
            Long problemId
    );
}

