
package com.shivacode.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_cases")
public class TestCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private ProblemEntity problem;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String input;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    @Column(nullable = false)
    private boolean hidden = false;

    public TestCaseEntity() {
    }

    public TestCaseEntity(
            ProblemEntity problem,
            String input,
            String expectedOutput,
            boolean hidden
    ) {
        this.problem = problem;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.hidden = hidden;
    }

    public Long getId() {
        return id;
    }

    public ProblemEntity getProblem() {
        return problem;
    }

    public String getInput() {
        return input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setProblem(ProblemEntity problem) {
        this.problem = problem;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}

