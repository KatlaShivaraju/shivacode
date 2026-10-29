
package com.shivacode.backend.controller;

import com.shivacode.backend.dto.CodeExecutionRequest;
import com.shivacode.backend.dto.CodeExecutionResponse;
import com.shivacode.backend.service.CodeExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/execute")
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;

    public CodeExecutionController(CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    @PostMapping
    public ResponseEntity<CodeExecutionResponse> executeCode(
            @Valid @RequestBody CodeExecutionRequest request) {

        CodeExecutionResponse response =
                codeExecutionService.execute(request);

        return ResponseEntity.ok(response);
    }
}

