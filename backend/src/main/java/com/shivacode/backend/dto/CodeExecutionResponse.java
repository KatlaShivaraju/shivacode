
package com.shivacode.backend.dto;

public record CodeExecutionResponse(
        String status,
        String output,
        String error,
        long executionTimeMs
) {
}
