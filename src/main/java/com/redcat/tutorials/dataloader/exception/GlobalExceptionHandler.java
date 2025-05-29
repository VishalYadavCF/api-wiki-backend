package com.redcat.tutorials.dataloader.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Exception handler for MethodNotFoundException
     */
    @ExceptionHandler(MethodNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotFoundException(MethodNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Method not found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Exception handler for ProjectNotFoundException
     */
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProjectNotFoundException(ProjectNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Project not found");
        response.put("message", ex.getMessage());

        log.warn("Project not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Exception handler for ControllerMethodNotFoundException
     */
    @ExceptionHandler(ControllerMethodNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleControllerMethodNotFoundException(ControllerMethodNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Controller method not found");
        response.put("message", ex.getMessage());

        log.warn("Controller method not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
