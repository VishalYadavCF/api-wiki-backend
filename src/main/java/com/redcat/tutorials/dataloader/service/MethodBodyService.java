package com.redcat.tutorials.dataloader.service;

import com.redcat.tutorials.dataloader.dto.*;
import com.redcat.tutorials.dataloader.exception.ControllerMethodNotFoundException;
import com.redcat.tutorials.dataloader.exception.ProjectNotFoundException;
import com.redcat.tutorials.dataloader.model.ApiMethodBody;
import com.redcat.tutorials.dataloader.repository.ApiMethodBodyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MethodBodyService {

    private final ApiMethodBodyRepository apiMethodBodyRepository;

    @Autowired
    public MethodBodyService(ApiMethodBodyRepository apiMethodBodyRepository) {
        this.apiMethodBodyRepository = apiMethodBodyRepository;
    }

    /**
     * Get all method bodies for a project
     *
     * @param projectName Name of the project
     * @return List of method body responses
     */
    public List<MethodBodyResponse> getAllMethodBodiesForProject(String projectName) {
        log.info("Retrieving all method bodies for project: {}", projectName);

        // Check if project exists
        if (!apiMethodBodyRepository.existsByProjectName(projectName)) {
            log.warn("Project not found: {}", projectName);
            throw new ProjectNotFoundException(projectName);
        }

        // Get all method bodies for the project
        List<ApiMethodBody> apiMethodBodies = apiMethodBodyRepository.findByProjectName(projectName);

        // Convert to response DTOs
        return apiMethodBodies.stream()
                .map(this::convertToMethodBodyResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all method bodies for a project with pagination and sorting
     *
     * @param projectName Name of the project
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (ASC or DESC)
     * @return PaginatedResponse containing method body responses
     */
    public PaginatedResponse<MethodBodyResponse> getAllMethodBodiesForProjectPaginated(
            String projectName, int page, int size, String sortBy, String sortDirection) {

        log.info("Retrieving paginated method bodies for project: {}, page: {}, size: {}",
                projectName, page, size);

        // Check if project exists
        if (!apiMethodBodyRepository.existsByProjectName(projectName)) {
            log.warn("Project not found: {}", projectName);
            throw new ProjectNotFoundException(projectName);
        }

        // Create sort and pageable objects
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get paginated method bodies
        Page<ApiMethodBody> methodBodiesPage = apiMethodBodyRepository.findByProjectName(projectName, pageable);

        // Convert to response DTOs
        List<MethodBodyResponse> methodBodyResponses = methodBodiesPage.getContent().stream()
                .map(this::convertToMethodBodyResponse)
                .collect(Collectors.toList());

        // Build the paginated response
        PaginatedResponse<MethodBodyResponse> response = new PaginatedResponse<>();
        response.setContent(methodBodyResponses);
        response.setPageNumber(methodBodiesPage.getNumber());
        response.setPageSize(methodBodiesPage.getSize());
        response.setTotalElements(methodBodiesPage.getTotalElements());
        response.setTotalPages(methodBodiesPage.getTotalPages());
        response.setLast(methodBodiesPage.isLast());

        return response;
    }

    /**
     * Get method body for a specific controller method in a project
     *
     * @param projectName Name of the project
     * @param controllerMethod Controller method path
     * @return Method body response
     */
    public MethodBodyResponse getMethodBodyForController(String projectName, String controllerMethod) {
        log.info("Retrieving method body for controller: {} in project: {}", controllerMethod, projectName);

        // Check if project exists
        if (!apiMethodBodyRepository.existsByProjectName(projectName)) {
            log.warn("Project not found: {}", projectName);
            throw new ProjectNotFoundException(projectName);
        }

        // Get method body for the controller
        return apiMethodBodyRepository.findByProjectNameAndControllerMethod(projectName, controllerMethod)
                .map(this::convertToMethodBodyResponse)
                .orElseThrow(() -> {
                    log.warn("Controller method not found: {} in project: {}", controllerMethod, projectName);
                    return new ControllerMethodNotFoundException(controllerMethod, projectName);
                });
    }

    /**
     * Convert ApiMethodBody to MethodBodyResponse
     *
     * @param apiMethodBody API method body entity
     * @return Method body response DTO
     */
    private MethodBodyResponse convertToMethodBodyResponse(ApiMethodBody apiMethodBody) {
        MethodBodyResponse response = new MethodBodyResponse();
        response.setProjectName(apiMethodBody.getProjectName());
        response.setControllerMethod(apiMethodBody.getControllerMethod());
        response.setFound(true);

        // Convert method details
        List<MethodBodyResponse.MethodDetailResponse> methodDetailResponses = apiMethodBody.getMethods().stream()
                .map(detail -> {
                    MethodBodyResponse.MethodDetailResponse responseDetail = new MethodBodyResponse.MethodDetailResponse();
                    responseDetail.setName(detail.getName());
                    responseDetail.setBody(detail.getBody());
                    responseDetail.setFilePath(detail.getFilePath());
                    return responseDetail;
                })
                .collect(Collectors.toList());

        response.setMethods(methodDetailResponses);
        response.setMessage("Method body retrieved successfully");

        return response;
    }


    public List<ApiMethodBody> getAllTheControllerEndpoints(String projectName) {
        return apiMethodBodyRepository.findByProjectName(projectName).stream()
                .collect(Collectors.toList());
    }

    private MethodDetailsResponse convertToMethodDetails(ApiMethodBody apiMethodBody) {
        return MethodDetailsResponse.builder()
                .methodName(apiMethodBody.getControllerMethod())
                .filePath(apiMethodBody.getMethods().get(0).getFilePath())
                .methodBody(apiMethodBody.getMethods().get(0).getBody())
                .build();
    }

    public MethodBodyResponse getAllMethodBodiesForCallGraph(String projectName, String endpointMethod) {
        log.info("Retrieving all method bodies for call graph for project: {}, endpoint: {}", projectName, endpointMethod);

        // Check if project exists
        if (!apiMethodBodyRepository.existsByProjectName(projectName)) {
            log.warn("Project not found: {}", projectName);
            throw new ProjectNotFoundException(projectName);
        }

        // Get method bodies for the specified endpoint
        Optional<List<ApiMethodBody>> apiMethodBodies = apiMethodBodyRepository.findByProjectNameAndControllerMethodLikeIgnoreCase(projectName, "*"+endpointMethod+"*");

        // Convert to response DTOs
        return apiMethodBodies.map(apiMethodBody -> {
            MethodBodyResponse response = convertToMethodBodyResponse(apiMethodBody.get(0));
            response.setMessage("Method body retrieved successfully for call graph");
            return response;
        }).orElseThrow(() -> {
            log.warn("Controller method not found: {} in project: {}", endpointMethod, projectName);
            return new ControllerMethodNotFoundException(endpointMethod, projectName);
        });
    }

    /**
     * Validates if any document exists with the specified project name
     *
     * @param projectName Name of the project to validate
     * @return true if project exists, false otherwise
     */
    public boolean validateProjectExists(String projectName) {
        log.info("Validating existence of project: {}", projectName);

        if (projectName == null || projectName.trim().isEmpty()) {
            log.warn("Project name is null or empty");
            return false;
        }

        boolean exists = apiMethodBodyRepository.existsByProjectName(projectName);

        if (exists) {
            log.info("Project exists: {}", projectName);
        } else {
            log.warn("Project does not exist: {}", projectName);
        }

        return exists;
    }

    /**
     * Count the total number of controller endpoints for a project
     *
     * @param projectName Name of the project
     * @return Total number of controller endpoints or 0 if project doesn't exist
     */
    public int countTotalControllerEndpoints(String projectName) {
        log.info("Counting total controller endpoints for project: {}", projectName);
        
        if (projectName == null || projectName.trim().isEmpty()) {
            log.warn("Project name is null or empty");
            return 0;
        }
        
        // Check if project exists
        if (!apiMethodBodyRepository.existsByProjectName(projectName)) {
            log.warn("Project not found when counting endpoints: {}", projectName);
            return 0;
        }
        
        // Get all controller methods for the project and count them
        List<ApiMethodBody> apiMethodBodies = apiMethodBodyRepository.findByProjectName(projectName);
        int count = apiMethodBodies.size();
        
        log.info("Found {} controller endpoints for project: {}", count, projectName);
        return count;
    }
}
