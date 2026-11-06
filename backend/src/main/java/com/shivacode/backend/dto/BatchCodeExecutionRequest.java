package com.shivacode.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchCodeExecutionRequest(

        @NotBlank(message = "Code cannot be empty")
        @Size(max = 102400, message = "Code must not exceed 100 KB")
        String code,

        @NotBlank(message = "Language is required")
        String language,

        @Size(max = 100, message = "Maximum 100 test cases allowed")
        List<
                @Size(max = 65536, message = "Each input must not exceed 64 KB")
                        String
                > inputs

) {
}
