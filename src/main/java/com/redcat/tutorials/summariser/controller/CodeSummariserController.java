package com.redcat.tutorials.summariser.controller;

import com.redcat.tutorials.summariser.dto.summary.CodeSummaryStatusResponse;
import com.redcat.tutorials.summariser.service.CodeSummariserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/code-summarizer")
@Slf4j
public class CodeSummariserController {

    private CodeSummariserService codeSummariserService;

    public CodeSummariserController(CodeSummariserService codeSummariserService) {
        this.codeSummariserService = codeSummariserService;
    }

    // create endpoint to start the process of summarising code
    @PostMapping("/initiate/{projectName}")
    public ResponseEntity<CodeSummaryStatusResponse> summariseCode(@PathVariable String projectName) {
        CodeSummaryStatusResponse response = codeSummariserService.summariseCode(projectName);
        return ResponseEntity.ok(response);
    }

    // create endpoint to get the status of the summarisation process
    @GetMapping("/status/{id}")
    public ResponseEntity<CodeSummaryStatusResponse> getSummarisationStatus(@PathVariable String id) {
        CodeSummaryStatusResponse response = codeSummariserService.getSummarisationStatus(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/retry/{projectName}")
    public Mono<Boolean> retryFailedSummary(
            @PathVariable(name = "project", required = false) String projectName) {

        log.info("Received request to retry a failed summary" +
                (projectName != null ? " for project: " + projectName : ""));

        return codeSummariserService.retryFailedSummary(projectName);
    }
}
// /api/code-summarizer/initiate/commonauth
