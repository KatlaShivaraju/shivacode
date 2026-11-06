
package com.shivacode.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeExecutionRequest(

        @NotBlank(message = "Code cannot be empty")
        @Size(max = 102400, message = "Code must not exceed 100 KB")
        String code,

        @NotBlank(message = "Language is required")
        String language,

        @Size(max = 65536, message = "Input must not exceed 64 KB")
        String input

) {
}



