package com.redcat.tutorials.embedder.controller;

import com.redcat.tutorials.embedder.dto.SearchQuery;
import com.redcat.tutorials.embedder.dto.SearchResponse;
import com.redcat.tutorials.embedder.service.SummaryEmbeddingService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/summary-embedder")
public class SummaryEmbedderController {

    private final SummaryEmbeddingService embeddingService;

    public SummaryEmbedderController(SummaryEmbeddingService summaryEmbeddingService) {
        this.embeddingService = summaryEmbeddingService;
    }

    @PostMapping("/initiate/{projectName}")
    public Mono<Map<String, Integer>> initiate(@PathVariable String projectName) {
        return embeddingService.initiateEmbeddings(projectName);
    }

    @PostMapping("/search")
    public Mono<SearchResponse> search(@RequestBody SearchQuery request) {
        return embeddingService.search(request);
    }
}
