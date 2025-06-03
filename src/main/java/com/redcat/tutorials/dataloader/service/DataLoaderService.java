package com.redcat.tutorials.dataloader.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redcat.tutorials.dataloader.dto.LoadDataResponse;
import com.redcat.tutorials.dataloader.model.ApiMethodBody;
import com.redcat.tutorials.dataloader.model.FullCallGraph;
import com.redcat.tutorials.dataloader.model.MethodDetail;
import com.redcat.tutorials.dataloader.repository.ApiMethodBodyRepository;
import com.redcat.tutorials.dataloader.repository.FullCallGraphRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DataLoaderService {

    private final ApiMethodBodyRepository apiMethodBodyRepository;
    private final FullCallGraphRepository fullCallGraphRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DataLoaderService(
            ApiMethodBodyRepository apiMethodBodyRepository,
            FullCallGraphRepository fullCallGraphRepository,
            ObjectMapper objectMapper) {
        this.apiMethodBodyRepository = apiMethodBodyRepository;
        this.fullCallGraphRepository = fullCallGraphRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Load data from JSON files into MongoDB
     * @param directoryPath Path to directory containing the JSON files (relative to project path)
     * @param projectName Name of the project for metadata
     * @return Response with status and counts of loaded data
     */
    public LoadDataResponse loadDataFromJsonFiles(String directoryPath, String projectName) {
        LoadDataResponse response = new LoadDataResponse();
        response.setSuccess(false);

        try {
            // Resolve the directory path relative to the project path
            Path dir = Paths.get(directoryPath).toAbsolutePath();

            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                response.setMessage("Directory not found: " + dir);
                return response;
            }

            // Load controller method bodies
            Path methodBodiesPath = dir.resolve("controller_method_bodies.json");
            if (Files.exists(methodBodiesPath)) {
                int methodBodiesCount = loadControllerMethodBodies(methodBodiesPath, projectName);
                response.setMethodBodiesLoaded(methodBodiesCount);
                log.info("Loaded {} controller method bodies", methodBodiesCount);
            } else {
                log.warn("controller_method_bodies.json not found in {}", dir);
            }

            // Load full call graph
            Path callGraphPath = dir.resolve("full_call_graph.json");
            if (Files.exists(callGraphPath)) {
                int callGraphCount = loadFullCallGraph(callGraphPath, projectName);
                response.setCallGraphNodesLoaded(callGraphCount);
                log.info("Loaded {} call graph nodes", callGraphCount);
            } else {
                log.warn("full_call_graph.json not found in {}", dir);
            }

            response.setSuccess(true);
            response.setMessage("Data loaded successfully");

        } catch (Exception e) {
            log.error("Error loading data", e);
            response.setMessage("Error loading data: " + e.getMessage());
        }

        return response;
    }

    private int loadControllerMethodBodies(Path filePath, String projectName) throws IOException {
        String content = Files.readString(filePath);
        log.debug("Reading JSON content from file: {}", filePath);

        // First try to parse the overall structure to understand it better
        JsonNode rootNode = objectMapper.readTree(content);

        List<ApiMethodBody> apiMethodBodies = new ArrayList<>();

        // Check if the root is an object with a "controllers" key
        if (rootNode.has("controllers") && rootNode.get("controllers").isArray()) {
            // Handle structure with "controllers" array
            JsonNode controllers = rootNode.get("controllers");
            for (JsonNode controller : controllers) {
                ApiMethodBody apiMethodBody = new ApiMethodBody();
                // Add null checks before accessing values
                if (controller.has("controllerMethod")) {
                    apiMethodBody.setControllerMethod(controller.get("controllerMethod").asText());
                } else {
                    log.warn("Controller missing name field, using placeholder");
                    apiMethodBody.setControllerMethod("unnamed-controller");
                }
                apiMethodBody.setProjectName(projectName);

                List<MethodDetail> methodDetails = new ArrayList<>();
                // Only process methods if they exist
                if (controller.has("methods")) {
                    JsonNode methods = controller.get("methods");

                    if (methods != null && methods.isArray()) {
                        for (JsonNode methodNode : methods) {
                             MethodDetail detail = new  MethodDetail();
                            detail.setName(methodNode.has("name") ? methodNode.get("name").asText() : "");
                            detail.setBody(methodNode.has("body") ? methodNode.get("body").asText() : "");
                            detail.setFilePath(methodNode.has("filePath") ? methodNode.get("filePath").asText() : "");
                            methodDetails.add(detail);
                        }
                    }
                } else {
                    log.warn("Controller missing methods field");
                }

                apiMethodBody.setMethods(methodDetails);
                apiMethodBodies.add(apiMethodBody);
            }
        } else {
            // Fall back to original approach for direct map structure
            try {
                Map<String, List<Map<String, String>>> methodBodiesMap =
                        objectMapper.readValue(content, new TypeReference<Map<String, List<Map<String, String>>>>() {});

                for (Map.Entry<String, List<Map<String, String>>> entry : methodBodiesMap.entrySet()) {
                    ApiMethodBody apiMethodBody = new ApiMethodBody();
                    apiMethodBody.setControllerMethod(entry.getKey());
                    apiMethodBody.setProjectName(projectName);

                    List< MethodDetail> methodDetails = new ArrayList<>();
                    for (Map<String, String> methodMap : entry.getValue()) {
                         MethodDetail detail = new  MethodDetail();
                        detail.setName(methodMap.get("name"));
                        detail.setBody(methodMap.get("body"));
                        detail.setFilePath(methodMap.get("filePath"));
                        methodDetails.add(detail);
                    }
                    apiMethodBody.setMethods(methodDetails);
                    apiMethodBodies.add(apiMethodBody);
                }
            } catch (JsonProcessingException e) {
                log.error("Error parsing JSON content as Map structure: {}", e.getMessage());
                throw e;
            }
        }

        apiMethodBodyRepository.saveAll(apiMethodBodies);
        return apiMethodBodies.size();
    }

    private int loadFullCallGraph(Path filePath, String projectName) throws IOException {
        String content = Files.readString(filePath);
        Map<String, List<String>> callGraphMap =
                objectMapper.readValue(content, new TypeReference<Map<String, List<String>>>() {});

        List<FullCallGraph> fullCallGraphs = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : callGraphMap.entrySet()) {
            FullCallGraph fullCallGraph = new FullCallGraph();
            fullCallGraph.setFullMethodPath(entry.getKey());
            fullCallGraph.setChildMethods(entry.getValue());
            fullCallGraph.setProjectName(projectName);
            fullCallGraphs.add(fullCallGraph);
        }

        fullCallGraphRepository.saveAll(fullCallGraphs);
        return fullCallGraphs.size();
    }
}
