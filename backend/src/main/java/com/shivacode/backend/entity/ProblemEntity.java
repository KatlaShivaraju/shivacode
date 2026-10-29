
package com.shivacode.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "problems")
public class ProblemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String difficulty;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String sampleInput;

    @Column(columnDefinition = "TEXT")
    private String sampleOutput;

    public ProblemEntity() {
    }

    public ProblemEntity(
            String title,
            String difficulty,
            String description,
            String sampleInput,
            String sampleOutput
    ) {
        this.title = title;
        this.difficulty = difficulty;
        this.description = description;
        this.sampleInput = sampleInput;
        this.sampleOutput = sampleOutput;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getDescription() {
        return description;
    }

    public String getSampleInput() {
        return sampleInput;
    }

    public String getSampleOutput() {
        return sampleOutput;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSampleInput(String sampleInput) {
        this.sampleInput = sampleInput;
    }

    public void setSampleOutput(String sampleOutput) {
        this.sampleOutput = sampleOutput;
    }
}

