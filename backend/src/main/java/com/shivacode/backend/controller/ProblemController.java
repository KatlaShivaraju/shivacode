
package com.shivacode.backend.controller;

import com.shivacode.backend.Problem;
import com.shivacode.backend.service.ProblemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }






    @GetMapping("/{id}")
    public Problem getProblemById(@PathVariable Long id) {
        return problemService.getProblemById(id);
    }


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


}

