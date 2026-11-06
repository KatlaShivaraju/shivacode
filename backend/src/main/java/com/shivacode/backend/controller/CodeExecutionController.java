
package com.shivacode.backend.controller;

import com.shivacode.backend.dto.CodeExecutionRequest;
import com.shivacode.backend.dto.CodeExecutionResponse;
import com.shivacode.backend.service.SecureCodeExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shivacode.backend.dto.BatchCodeExecutionRequest;
import java.util.List;

@RestController
@RequestMapping("/api/execute")
public class CodeExecutionController {

    private final SecureCodeExecutionService secureCodeExecutionService;

    public CodeExecutionController(
            SecureCodeExecutionService secureCodeExecutionService) {
        this.secureCodeExecutionService = secureCodeExecutionService;
    }

    @PostMapping
    public ResponseEntity<CodeExecutionResponse> executeCode(
            @Valid @RequestBody CodeExecutionRequest request) {

        CodeExecutionResponse response =
                secureCodeExecutionService.execute(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<CodeExecutionResponse>> executeBatch(
            @Valid @RequestBody BatchCodeExecutionRequest request) {

        CodeExecutionRequest executionRequest =
                new CodeExecutionRequest(
                        request.code(),
                        request.language(),
                        null
                );

        List<CodeExecutionResponse> responses =
                secureCodeExecutionService.executeBatch(
                        executionRequest,
                        request.inputs()
                );

        return ResponseEntity.ok(responses);
    }
}


