package com.redcat.tutorials.embedder.service;

import com.redcat.tutorials.dataloader.model.ApiMethodBody;
import com.redcat.tutorials.dataloader.model.MethodDetail;
import com.redcat.tutorials.dataloader.repository.ApiMethodBodyRepository;
import com.redcat.tutorials.embedder.dto.*;
import com.redcat.tutorials.summariser.model.CodeSummaryContentEntity;
import com.redcat.tutorials.summariser.model.CodeSummaryContentStatus;
import com.redcat.tutorials.summariser.repository.CodeSummaryContentRepository;
import com.redcat.tutorials.summariser.repository.CodeSummaryContentStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SummaryEmbeddingService {

    public static final String EMPTY = "";
    private final CodeSummaryContentRepository contentRepo;
    private final CodeSummaryContentStatusRepository contentSummaryRepository;
    private final VectorStore vectorStore;
    private final ApiMethodBodyRepository apiMethodBodyRepository;

    public SummaryEmbeddingService(CodeSummaryContentRepository contentRepo,
                                   CodeSummaryContentStatusRepository contentSummaryRepository,
                                   VectorStore vectorStore,
                                   ApiMethodBodyRepository apiMethodBodyRepository) {
        this.contentRepo = contentRepo;
        this.contentSummaryRepository = contentSummaryRepository;
        this.vectorStore = vectorStore;
        this.apiMethodBodyRepository = apiMethodBodyRepository;
    }

    private List<String> chunkText(String text, int maxTokens) {
        // Simple sentence-based chunking (approximate token counting)
        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentTokenCount = 0;

        for (String sentence : sentences) {
            int sentenceTokens = estimateTokenCount(sentence);

            if (currentTokenCount + sentenceTokens > maxTokens && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                currentTokenCount = 0;
            }

            currentChunk.append(sentence).append(" ");
            currentTokenCount += sentenceTokens;
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private int estimateTokenCount(String text) {
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }

    public Mono<Map<String, Integer>> initiateEmbeddings(String projectName) {
        return Mono.fromCallable(() -> {
            List<CodeSummaryContentEntity> all = contentRepo.findAll();
            int success = 0;
            int failed = 0;

            for (CodeSummaryContentEntity entity : all) {
                List<String> chunks = chunkText(entity.getSummary(), 500);

                for (int i = 0; i < chunks.size(); i++) {
                    String chunkId = entity.getCodeSummaryContentId() + "_chunk_" + i;
                    Document document = new Document(chunks.get(i),
                            Map.of(
                                    "content-status-id", entity.getCodeSummaryContentId(),
                                    "chunk-index", i,
                                    "total-chunks", chunks.size(),
                                    "content-summary-id", entity.getId()
                            ));
                    try {
                        vectorStore.add(List.of(document));
                        success += 1;
                    } catch (Exception e) {
                        failed += 1;
                        log.error("Failed to add chunk for entity {}: {}", entity.getCodeSummaryContentId(), e.getMessage());
                    }
                }
            }

            return Map.of("successCount", success, "failedCount", failed);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<SearchResponse> search(SearchQuery request) {
        return Mono.fromCallable(() -> {
            List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                    .similarityThreshold(request.getThreshold())
                    .topK(request.getTopK())
                    .query(request.getQuery())
                    .build());

            if(matches.isEmpty()) {
                log.info("No matches found for query: {}", request.getQuery());
                return SearchResponse.builder().results(Collections.emptyList()).build();
            }

            List<SearchResult> searchResults = matches.stream()
                    .map(document -> {
                        // Extract file path from document metadata
                        String filePath = null;
                        Map<String, Object> metadata = document.getMetadata();
                        String contentSummaryId = (String) metadata.get("content-status-id");
                        Optional<CodeSummaryContentStatus> cdOptional = contentSummaryRepository.findById(contentSummaryId);
                        if(cdOptional.isPresent()) {
                            log.info("Found content summary status for ID: {}", contentSummaryId);
                        }
                        String controllerMethod = cdOptional.get().getControllerMethod();
                        List<String> filePaths = new ArrayList<>();
                        Set<String> uniqueFilePaths = new HashSet<>();
                        Optional<ApiMethodBody> apiMethodBodyOptional = apiMethodBodyRepository.findByProjectNameAndControllerMethod(request.getProjectName(), controllerMethod);
                        if(apiMethodBodyOptional.isPresent()) {
                            apiMethodBodyOptional.get().getMethods().parallelStream().map(MethodDetail::getFilePath)
                                    .map(fPath -> fPath.replaceAll(".*/target/classes/(.+)\\.class$", "$1.java"))
                                    .filter(fPath -> !uniqueFilePaths.contains(fPath) && !EMPTY.equals(fPath))
                                    .forEach(fp -> {
                                        filePaths.add(fp);
                                        uniqueFilePaths.add(fp);
                                    });
                        } else {
                            log.warn("No API method body found for project: {} and controller method: {}", request.getProjectName(), controllerMethod);
                        }
                        Collections.reverse(filePaths);
                        return SearchResult.builder().filePaths(filePaths).contentSummaryId((String)document.getMetadata().get("content-summary-id")).build();
                    })
                    .collect(Collectors.toList());
            return SearchResponse.builder().results(searchResults).build();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
