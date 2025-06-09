package com.redcat.tutorials.summariser.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redcat.tutorials.dataloader.model.ApiMethodBody;
import com.redcat.tutorials.dataloader.model.MethodDetail;
import com.redcat.tutorials.dataloader.repository.ApiMethodBodyRepository;
import com.redcat.tutorials.dataloader.service.MethodBodyService;
import com.redcat.tutorials.summariser.dto.summary.CodeSummaryStatusResponse;
import com.redcat.tutorials.summariser.model.CodeSummaryContentEntity;
import com.redcat.tutorials.summariser.model.CodeSummaryContentStatus;
import com.redcat.tutorials.summariser.model.CodeSummaryStatus;
import com.redcat.tutorials.summariser.model.CodeSummaryStatusEntity;
import com.redcat.tutorials.summariser.repository.CodeSummaryContentRepository;
import com.redcat.tutorials.summariser.repository.CodeSummaryContentStatusRepository;
import com.redcat.tutorials.summariser.repository.CodeSummaryStatusRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.redcat.tutorials.summariser.constants.PromptTemplateConstants.CODE_SUMMARY_PROMPT_FOR_BUSINESS;

@Service
@Slf4j
public class CodeSummariserService {

    private final GoogleGeminiService googleGeminiService;
    private final CodeSummaryStatusRepo codeSummaryStatusRepo;
    private final CodeSummaryContentStatusRepository contentStatusRepository;
    private final CodeSummaryContentRepository contentRepository;
    private final ObjectMapper objectMapper;
    private final MethodBodyService methodBodyService;
    private final ApiMethodBodyRepository apiMethodBodyRepository;

    @Value("${google.api.key}")
    private String apiKey;

    public CodeSummariserService(GoogleGeminiService googleGeminiService,
                                 CodeSummaryStatusRepo codeSummaryStatusRepo,
                                 CodeSummaryContentStatusRepository contentStatusRepository,
                                 CodeSummaryContentRepository contentRepository,
                                 ObjectMapper objectMapper,
                                 MethodBodyService methodBodyService,
                                 ApiMethodBodyRepository apiMethodBodyRepository) {
        this.googleGeminiService = googleGeminiService;
        this.codeSummaryStatusRepo = codeSummaryStatusRepo;
        this.contentStatusRepository = contentStatusRepository;
        this.contentRepository = contentRepository;
        this.objectMapper = objectMapper;
        this.methodBodyService = methodBodyService;
        this.apiMethodBodyRepository = apiMethodBodyRepository;
    }

    public CodeSummaryStatusResponse summariseCode(String projectName) {

        // check if projectName is null or empty
        if (projectName == null || projectName.isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }

        // Check if the controller_method_bodies.json file exists for this project
        validateProjectName(projectName);

        Integer totalApiEndpoints = methodBodyService.countTotalControllerEndpoints(projectName);

        CodeSummaryStatusEntity entity = CodeSummaryStatusEntity.builder()
                .projectName(projectName)
                .apiEndpointsFinished(new ArrayList<>())
                .totalApiEndpoints(totalApiEndpoints)
                .status(CodeSummaryStatus.INITIATED)
                .success(null)
                .build();

        CodeSummaryStatusEntity savedEntity = codeSummaryStatusRepo.save(entity);

        prepareAndExecutePrompts(savedEntity.getId(), projectName);

        return CodeSummaryStatusResponse.builder()
                .id(savedEntity.getId())
                .projectName(savedEntity.getProjectName())
                .apiEndpointsFinished(savedEntity.getApiEndpointsFinished())
                .totalApiEndpoints(savedEntity.getTotalApiEndpoints())
                .status(savedEntity.getStatus().toString())
                .success(savedEntity.getSuccess())
                .build();
    }

    private void validateProjectName(String projectName) {
        if(!methodBodyService.validateProjectExists(projectName)) {
            log.error("Project with name {} does not exist", projectName);
            throw new IllegalArgumentException("Project with name " + projectName + " does not exist");
        }
    }

    @Async
    public void prepareAndExecutePrompts(String summaryId, String projectName) {
        log.info("Starting to prepare prompts for code summary with ID: {}", summaryId);

        try {
            CodeSummaryStatusEntity summaryEntity = codeSummaryStatusRepo.findById(summaryId)
                    .orElseThrow(() -> new IllegalArgumentException("Summary with ID " + summaryId + " not found"));
            summaryEntity.setStatus(CodeSummaryStatus.IN_PROGRESS);
            codeSummaryStatusRepo.save(summaryEntity);

            List<ApiMethodBody> apiMethodBodies = methodBodyService.getAllTheControllerEndpoints(projectName);
            log.info("Found {} endpoints for project {}", apiMethodBodies.size(), projectName);

            AtomicInteger completedCount = new AtomicInteger(0);
            List<Map<String, String>> finishedEndpoints = new ArrayList<>();
            createSummaryDirectory(projectName);

            // Wrap each endpoint into a Mono
            Flux.fromIterable(apiMethodBodies)
                    .concatMap(apiMethodBody ->
                            processEndpoint(summaryId, projectName,
                                    apiMethodBody.getControllerMethod(),
                                    apiMethodBody, completedCount, finishedEndpoints)
                    )
                    .doOnComplete(() -> {
                        // Mark summary as finished
                        CodeSummaryStatusEntity finishedSummary = codeSummaryStatusRepo.findById(summaryId).orElseThrow();
                        finishedSummary.setStatus(CodeSummaryStatus.FINISHED);
                        finishedSummary.setSuccess(true);
                        finishedSummary.setApiEndpointsFinished(finishedEndpoints);
                        codeSummaryStatusRepo.save(finishedSummary);

                        log.info("Completed code summary generation for project: {}, summary ID: {}", projectName, summaryId);
                    })
                    .doOnError(e -> {
                        log.error("Error generating code summaries for project {}: {}", projectName, e.getMessage(), e);
                        updateSummaryStatusOnFailure(summaryId);
                    })
                    .subscribe(); // Start the processing chain

        } catch (Exception e) {
            log.error("Error setting up code summary for project {}: {}", projectName, e.getMessage(), e);
            updateSummaryStatusOnFailure(summaryId);
        }
    }


    private Mono<Void> processEndpoint(String summaryId, String projectName, String controllerMethod,
                                       ApiMethodBody apiMethodBody, AtomicInteger completedCount,
                                       List<Map<String, String>> finishedEndpoints) {
        try {
            log.info("Processing endpoint: {}", controllerMethod);

            CodeSummaryContentStatus contentStatus = CodeSummaryContentStatus.builder()
                    .summaryId(summaryId)
                    .projectName(projectName)
                    .controllerMethod(controllerMethod)
                    .status(CodeSummaryStatus.IN_PROGRESS)
                    .build();

            CodeSummaryContentStatus savedContentStatus = contentStatusRepository.save(contentStatus);

            StringBuilder combinedMethodBody = new StringBuilder();
            for (MethodDetail method : apiMethodBody.getMethods()) {
                combinedMethodBody.append("Method: ").append(method.getName()).append("\n");
                combinedMethodBody.append("```\n").append(method.getBody()).append("\n```\n\n");
            }

            String prompt = preparePromptForCodeSummary(controllerMethod, combinedMethodBody.toString());

            return googleGeminiService.generateContent(apiKey, prompt)
                    .flatMap(response -> {
                        try {
                            String generatedSummary = googleGeminiService.extractResponseText(response);
                            saveGeneratedSummary(savedContentStatus.getId(), generatedSummary);
                            savedContentStatus.setStatus(CodeSummaryStatus.FINISHED);
                            contentStatusRepository.save(savedContentStatus);

                            Map<String, String> finishedEndpoint = Map.of(
                                    "controllerMethod", controllerMethod,
                                    "status", CodeSummaryStatus.FINISHED.toString()
                            );
                            finishedEndpoints.add(finishedEndpoint);

                            completedCount.incrementAndGet();
                            log.info("Generated summary for endpoint: {}, completed count: {}", controllerMethod, completedCount.get());
                            return Mono.empty();
                        } catch (Exception e) {
                            handleEndpointError(savedContentStatus, controllerMethod, e);
                            return Mono.empty();
                        }
                    })
                    .onErrorResume(error -> {
                        log.error("Error generating summary for endpoint {}: {}", controllerMethod, error.getMessage(), error);
                        handleEndpointError(savedContentStatus, controllerMethod, new RuntimeException(error));
                        finishedEndpoints.add(Map.of(
                                "controllerMethod", controllerMethod,
                                "status", CodeSummaryStatus.FAILED.toString()
                        ));
                        completedCount.incrementAndGet();
                        return Mono.empty();
                    }).then();
        } catch (Exception e) {
            log.error("Error processing endpoint {}: {}", controllerMethod, e.getMessage(), e);
            return Mono.empty();
        }
    }


    private void handleEndpointError(CodeSummaryContentStatus contentStatus, String controllerMethod, Exception e) {
        log.error("Error handling endpoint summary for {}: {}", controllerMethod, e.getMessage(), e);
        contentStatus.setStatus(CodeSummaryStatus.FAILED);
        contentStatusRepository.save(contentStatus);
    }

    private void saveGeneratedSummary(String contentStatusId, String summary) {
        CodeSummaryContentEntity contentEntity = CodeSummaryContentEntity.builder()
                .codeSummaryContentId(contentStatusId)
                .summary(summary)
                .build();

        contentRepository.save(contentEntity);
    }

    private void createSummaryDirectory(String projectName) {
        try {
            Path dirPath = Paths.get("business-summary", projectName);
            Files.createDirectories(dirPath);
            log.info("Created directory: {}", dirPath);
        } catch (IOException e) {
            log.error("Error creating directory for project {}: {}", projectName, e.getMessage(), e);
            throw new RuntimeException("Failed to create summary directory", e);
        }
    }

    private void writeSummaryToFile(String id, String projectName, String controllerMethod, String summary) {
        try {
            // Clean up controller method name to use as filename
            String fileName = controllerMethod.replaceAll("[^a-zA-Z0-9]", "_") + "_"+ id + ".md";
            Path filePath = Paths.get("business-summary", projectName, fileName);

            Files.writeString(filePath, summary);
            log.info("Wrote summary to file: {}", filePath);
        } catch (IOException e) {
            log.error("Error writing summary to file for {}: {}", controllerMethod, e.getMessage(), e);
        }
    }

    private void updateSummaryStatusOnFailure(String summaryId) {
        try {
            CodeSummaryStatusEntity summaryEntity = codeSummaryStatusRepo.findById(summaryId).orElse(null);
            if (summaryEntity != null) {
                summaryEntity.setStatus(CodeSummaryStatus.FAILED);
                summaryEntity.setSuccess(false);
                codeSummaryStatusRepo.save(summaryEntity);
                log.info("Updated summary status to FAILED for ID: {}", summaryId);
            }
        } catch (Exception e) {
            log.error("Error updating summary status on failure: {}", e.getMessage(), e);
        }
    }

    private String preparePromptForCodeSummary(String apiEndpoint, String content) {
        return String.format(CODE_SUMMARY_PROMPT_FOR_BUSINESS, apiEndpoint, content);
    }

    public CodeSummaryStatusResponse getSummarisationStatus(String id) {
        // Existing method stub
        return new CodeSummaryStatusResponse();
    }

    /**
     * Retry a failed code summary. Finds one failed entry and attempts to regenerate its summary.
     *
     * @param projectName Optional project name to filter by (can be null to search across all projects)
     * @return Status of the retry operation with details about the retried entry
     */
    public Mono<Boolean> retryFailedSummary(String projectName) {
        log.info("Looking for a failed summary to retry" + (projectName != null ? " for project: " + projectName : ""));

        // Find a failed content status entry
        Optional<CodeSummaryContentStatus> failedContentOpt = projectName != null ?
                contentStatusRepository.findFirstByProjectNameAndStatus(projectName, CodeSummaryStatus.FAILED) :
                contentStatusRepository.findFirstByStatus(CodeSummaryStatus.FAILED);

        if (failedContentOpt.isEmpty()) {
            log.info("No failed summaries found" + (projectName != null ? " for project: " + projectName : ""));
            return Mono.just(false);
        }

        CodeSummaryContentStatus failedContent = failedContentOpt.get();
        String controllerMethod = failedContent.getControllerMethod();
        String summaryId = failedContent.getSummaryId();
        String contentProjectName = failedContent.getProjectName();

        log.info("Found failed summary for endpoint: {}, attempting retry", controllerMethod);

        try {
            // Update status to IN_PROGRESS
            failedContent.setStatus(CodeSummaryStatus.IN_PROGRESS);
            contentStatusRepository.save(failedContent);

            // Find the method body for this controller method
            Optional<ApiMethodBody> apiMethodBodyOpt = apiMethodBodyRepository.findByProjectNameAndControllerMethod(
                    contentProjectName, controllerMethod);

            if (apiMethodBodyOpt.isEmpty()) {
                log.error("Could not find method body for controller: {}", controllerMethod);
                failedContent.setStatus(CodeSummaryStatus.FAILED);
                contentStatusRepository.save(failedContent);
                return Mono.just(false);
            }

            ApiMethodBody apiMethodBody = apiMethodBodyOpt.get();

            // Combine all method bodies into a single string
            StringBuilder combinedMethodBody = new StringBuilder();
            for (MethodDetail method : apiMethodBody.getMethods()) {
                combinedMethodBody.append("Method: ").append(method.getName()).append("\n");
                combinedMethodBody.append("```\n").append(method.getBody()).append("\n```\n\n");
            }

            // Generate the prompt for this endpoint
            String prompt = preparePromptForCodeSummary(controllerMethod, combinedMethodBody.toString());

            // Call Gemini API to generate the summary in a reactive way (no blocking)
            // Call Gemini API to generate the summary in a non-blocking way
            googleGeminiService.generateContent(apiKey, prompt)
                    .subscribe(response -> {
                        try {
                            String generatedSummary = googleGeminiService.extractResponseText(response);

                            // Save the generated summary
                            saveGeneratedSummary(failedContent.getId(), generatedSummary);

                            // Write summary to a markdown file
                            // writeSummaryToFile(failedContent.getId(), contentProjectName, controllerMethod, generatedSummary);

                            // Update status to FINISHED
                            failedContent.setStatus(CodeSummaryStatus.FINISHED);
                            contentStatusRepository.save(failedContent);

                            // Update finished endpoints list
                            Map<String, String> finishedEndpoint = Map.of(
                                    "controllerMethod", controllerMethod,
                                    "status", CodeSummaryStatus.FINISHED.toString()
                            );
                        } catch (Exception e) {
                            handleEndpointError(failedContent, controllerMethod, e);
                        }
                    }, error -> {
                        log.error("Error generating summary for endpoint {}: {}", controllerMethod, error.getMessage(), error);
                        handleEndpointError(failedContent, controllerMethod, new RuntimeException(error));
                    });

            return Mono.just(true);

        } catch (Exception e) {
            log.error("Error during retry for endpoint {}: {}", controllerMethod, e.getMessage(), e);
            return Mono.just(false);
        }
    }
}
