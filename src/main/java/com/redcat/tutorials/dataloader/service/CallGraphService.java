package com.redcat.tutorials.dataloader.service;

import com.redcat.tutorials.dataloader.dto.CallGraphResponse;
import com.redcat.tutorials.dataloader.dto.PaginatedResponse;
import com.redcat.tutorials.dataloader.exception.MethodNotFoundException;
import com.redcat.tutorials.dataloader.exception.ProjectNotFoundException;
import com.redcat.tutorials.dataloader.model.FullCallGraph;
import com.redcat.tutorials.dataloader.repository.ApiMethodBodyRepository;
import com.redcat.tutorials.dataloader.repository.FullCallGraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CallGraphService {
    private static final Logger logger = LoggerFactory.getLogger(CallGraphService.class);

    private final ApiMethodBodyRepository apiMethodBodyRepository;
    private final FullCallGraphRepository fullCallGraphRepository;

    @Autowired
    public CallGraphService(
            ApiMethodBodyRepository apiMethodBodyRepository,
            FullCallGraphRepository fullCallGraphRepository) {
        this.apiMethodBodyRepository = apiMethodBodyRepository;
        this.fullCallGraphRepository = fullCallGraphRepository;
    }

    /**
     * Get call graph for a method in a project
     *
     * @param projectName Name of the project
     * @param methodName Full name of the method (e.g., com.example.Controller.method)
     * @return Call graph response with direct and transitive method calls
     */
    public CallGraphResponse getMethodCallGraph(String projectName, String methodName) {
        // Verify project exists
        if (!fullCallGraphRepository.existsByProjectName(projectName)) {
            throw new ProjectNotFoundException(projectName);
        }

        // Find the method in the project's call graph
        List<FullCallGraph> methodNode = fullCallGraphRepository
                .findByProjectNameAndFullMethodPathLike(projectName, methodName)
                .orElseThrow(() -> new MethodNotFoundException(methodName, projectName));

        // Build the call graph response
        CallGraphResponse response = new CallGraphResponse();
        response.setProjectName(projectName);
        response.setMethodName(methodName);

// Aggregate all direct calls from the list of method nodes
        List<String> directCalls = methodNode.stream()
                .flatMap(node -> node.getChildMethods().stream())
                .distinct()
                .collect(Collectors.toList());
        response.setDirectCalls(directCalls);
        response.setFound(true);

// Generate the transitive call graph
        Map<String, List<String>> fullGraph = buildTransitiveCallGraph(projectName, methodName);
        response.setFullCallGraph(fullGraph);
        response.setMessage("Call graph retrieved successfully");

        return response;
    }

    /**
     * Find all methods that call a given method
     *
     * @param projectName Name of the project
     * @param methodName Method being called
     * @return List of methods that call the specified method
     */
    public List<String> findCallers(String projectName, String methodName) {
        // Verify project exists
        if (!fullCallGraphRepository.existsByProjectName(projectName)) {
            throw new ProjectNotFoundException(projectName);
        }

        // Find all methods that contain the target method in their child methods
        List<FullCallGraph> callerNodes = fullCallGraphRepository
                .findByProjectNameAndChildMethodsContaining(projectName, methodName);

        return callerNodes.stream()
                .map(FullCallGraph::getFullMethodPath)
                .collect(Collectors.toList());
    }

    /**
     * Find all methods that call a given method with pagination
     *
     * @param projectName Name of the project
     * @param methodName Method being called
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (ASC or DESC)
     * @return PaginatedResponse containing methods that call the specified method
     */
    public PaginatedResponse<String> findCallersPaginated(
            String projectName, String methodName, int page, int size, String sortBy, String sortDirection) {

        // Verify project exists
        if (!fullCallGraphRepository.existsByProjectName(projectName)) {
            throw new ProjectNotFoundException(projectName);
        }

        // Create sort and pageable objects
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Find all methods that contain the target method in their child methods with pagination
        Page<FullCallGraph> callerNodesPage = fullCallGraphRepository
                .findByProjectNameAndChildMethodsContaining(projectName, methodName, pageable);

        List<String> callers = callerNodesPage.getContent().stream()
                .map(FullCallGraph::getFullMethodPath)
                .collect(Collectors.toList());

        // Build the paginated response
        PaginatedResponse<String> response = new PaginatedResponse<>();
        response.setContent(callers);
        response.setPageNumber(callerNodesPage.getNumber());
        response.setPageSize(callerNodesPage.getSize());
        response.setTotalElements(callerNodesPage.getTotalElements());
        response.setTotalPages(callerNodesPage.getTotalPages());
        response.setLast(callerNodesPage.isLast());

        return response;
    }

    /**
     * Get paginated call graph nodes for a project
     *
     * @param projectName Name of the project
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (ASC or DESC)
     * @return PaginatedResponse containing call graph nodes
     */
    public PaginatedResponse<FullCallGraph> getCallGraphNodesPaginated(
            String projectName, int page, int size, String sortBy, String sortDirection) {

        // Verify project exists
        if (!fullCallGraphRepository.existsByProjectName(projectName)) {
            throw new ProjectNotFoundException(projectName);
        }

        // Create sort and pageable objects
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Find all call graph nodes for the project with pagination
        Page<FullCallGraph> nodesPage = fullCallGraphRepository.findByProjectName(projectName, pageable);

        // Build the paginated response
        PaginatedResponse<FullCallGraph> response = new PaginatedResponse<>();
        response.setContent(nodesPage.getContent());
        response.setPageNumber(nodesPage.getNumber());
        response.setPageSize(nodesPage.getSize());
        response.setTotalElements(nodesPage.getTotalElements());
        response.setTotalPages(nodesPage.getTotalPages());
        response.setLast(nodesPage.isLast());

        return response;
    }

    /**
     * Build a transitive call graph starting from a method
     *
     * @param projectName Name of the project
     * @param methodName Starting method
     * @return Map representing the full call graph
     */
    private Map<String, List<String>> buildTransitiveCallGraph(String projectName, String methodName) {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        // Start with the specified method
        queue.add(methodName);

        while (!queue.isEmpty()) {
            String currentMethod = queue.poll();

            // Skip if already processed
            if (visited.contains(currentMethod)) {
                continue;
            }

            // Mark as visited
            visited.add(currentMethod);

            // Get direct calls for this method
            Optional<FullCallGraph> methodNodeOpt =
                    fullCallGraphRepository.findByProjectNameAndFullMethodPath(projectName, currentMethod);

            if (methodNodeOpt.isPresent()) {
                FullCallGraph methodNode = methodNodeOpt.get();
                List<String> childMethods = methodNode.getChildMethods();

                // Add to graph
                graph.put(currentMethod, childMethods);

                // Queue up child methods for processing
                queue.addAll(childMethods);
            } else {
                // Method exists in the call graph as a child but doesn't have its own entry
                graph.put(currentMethod, Collections.emptyList());
            }
        }

        return graph;
    }

    /**
     * Get list of all projects with call graphs
     *
     * @return List of project names
     */
    public List<String> getAllProjects() {
        return fullCallGraphRepository.findAll().stream()
                .map(FullCallGraph::getProjectName)
                .distinct()
                .collect(Collectors.toList());
    }
}
