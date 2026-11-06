
package com.shivacode.backend.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Resource not found
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            NoSuchElementException exception
    ) {
        String message = exception.getMessage() != null
                ? exception.getMessage()
                : "Resource not found.";

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", message));
    }

    // 400 - Request validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception
    ) {
        String errorMessage = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .orElse("Invalid request.");

        return ResponseEntity
                .badRequest()
                .body(Map.of("error", errorMessage));
    }

    // 400 - Constraint violations
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", "Request validation failed."));
    }

    // 400 - Invalid JSON request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", "Invalid or malformed JSON request."));
    }

    // 400 - Invalid parameter type
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", "Invalid parameter value."));
    }

    // 400 - Invalid arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        String message = exception.getMessage() != null
                ? exception.getMessage()
                : "Invalid request.";

        return ResponseEntity
                .badRequest()
                .body(Map.of("error", message));
    }
}



