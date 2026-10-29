
package com.shivacode.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeExecutionRequest(
        @NotBlank(message = "Code cannot be empty")
        @Size(max = 20000, message = "Code must not exceed 20,000 characters")
        String code,

        @NotBlank(message = "Language is required")
        String language,

        @Size(max = 10000, message = "Input must not exceed 10,000 characters")
        String input
) {
}


