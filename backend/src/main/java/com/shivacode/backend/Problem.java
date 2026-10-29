package com.shivacode.backend;

public record Problem(   Long id,
                        String title,
                        String difficulty,
                        String description,
                        String sampleInput,
                        String sampleOutput) {
}
