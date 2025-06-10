package com.redcat.tutorials.web.controller;

import com.redcat.tutorials.web.service.ApiWikiProjectService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import com.redcat.tutorials.web.model.CodeSummaryResponseDto;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/wiki/content")
public class ApiWikiContentController {

    /**
     * 1. Fetch Index ->
     *
     * a) Controller Summary
     * b) Design Patterns
     * c) Class Dependencies Summary / LLD architecture
     * d) Schema Design Summary -> Either MongoDB or RDS
     * e) Class Level Summary -> Only For Service Classes annotated with @Service
     * f) Config Summary -> Only For Config Classes annotated with @Configuration
     * g) External Service Interaction -> REST or Grpc or SQS or Kafka
     * h) Unit Test Class Summary -> Only For Test Classes present inside src/test/java
     *
     * 2. Fetch Content by ID ->
     *
     * 3. Fetch Controller Summary by ID ->
     *
     * 4. Fetch Class Level Summary by ID ->
     *
     * 5. Fetch Test Class Summary by ID ->
     */

    private final ApiWikiProjectService apiWikiProjectService;

    public ApiWikiContentController(ApiWikiProjectService apiWikiProjectService) {
        this.apiWikiProjectService = apiWikiProjectService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodeSummaryResponseDto> getContentById(@PathVariable("id") String id) {
        CodeSummaryResponseDto response = apiWikiProjectService.getContentById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectName}/api-summaries")
    public ResponseEntity<List<CodeSummaryResponseDto>> getAllControllerSummariesForAProject(@PathVariable("projectName") String projectName) {
        List<CodeSummaryResponseDto> summaries = apiWikiProjectService.getAllControllerSummariesForAProject(projectName);
        return ResponseEntity.ok(summaries);
    }

}
