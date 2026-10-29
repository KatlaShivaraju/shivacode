
package com.shivacode.backend.service;

import com.shivacode.backend.Problem;
import com.shivacode.backend.entity.ProblemEntity;
import com.shivacode.backend.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;




import java.util.List;


@Service
public class ProblemService {

    private final ProblemRepository problemRepository;

    public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

//    public List<Problem> getAllProblems() {
//        return problemRepository.findAll()
//                .stream()
//                .map(this::toProblem)
//                .toList();
//    }

    public Page<Problem> getAllProblems(Pageable pageable) {
        return problemRepository.findAll(pageable)
                .map(this::toProblem);
    }


    public Problem getProblemById(Long id) {
        ProblemEntity entity = problemRepository.findById(id)
                .orElseThrow(() ->
                        new java.util.NoSuchElementException(
                                "Problem not found with id: " + id
                        )
                );

        return toProblem(entity);
    }


    private Problem toProblem(ProblemEntity entity) {
        return new Problem(
                entity.getId(),
                entity.getTitle(),
                entity.getDifficulty(),
                entity.getDescription(),
                entity.getSampleInput(),
                entity.getSampleOutput()
        );
    }


    public Page<Problem> getProblemsByDifficulty(
            String difficulty,
            Pageable pageable
    ) {
        return problemRepository
                .findByDifficultyIgnoreCase(difficulty, pageable)
                .map(this::toProblem);
    }

}


