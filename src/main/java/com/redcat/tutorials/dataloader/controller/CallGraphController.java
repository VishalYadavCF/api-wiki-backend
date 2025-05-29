package com.redcat.tutorials.dataloader.controller;

import com.redcat.tutorials.dataloader.dto.CallGraphResponse;
import com.redcat.tutorials.dataloader.dto.PaginatedResponse;
import com.redcat.tutorials.dataloader.exception.MethodNotFoundException;
import com.redcat.tutorials.dataloader.exception.ProjectNotFoundException;
import com.redcat.tutorials.dataloader.model.FullCallGraph;
import com.redcat.tutorials.dataloader.service.CallGraphService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/callgraph")
@Slf4j
public class CallGraphController {

    private static final Logger logger = LoggerFactory.getLogger(CallGraphController.class);
    private final CallGraphService callGraphService;

    @Autowired
    public CallGraphController(CallGraphService callGraphService) {
        this.callGraphService = callGraphService;
    }

    /**
     * Get call graph for a method in a project
     *
     * @param projectName Name of the project
     * @param methodName Full name of the method
     * @return Call graph response
     */
    @GetMapping("/{projectName}/methods/{methodName}")
    public ResponseEntity<CallGraphResponse> getMethodCallGraph(
            @PathVariable String projectName,
            @PathVariable String methodName) {

        log.info("Fetching call graph for method: {} in project: {}", methodName, projectName);
        CallGraphResponse response = callGraphService.getMethodCallGraph(projectName, methodName);
        return ResponseEntity.ok(response);
    }

    /**
     * Find all methods that call a given method
     *
     * @param projectName Name of the project
     * @param methodName Method being called
     * @return List of methods that call the specified method
     */
    @GetMapping("/{projectName}/callers/{methodName}")
    public ResponseEntity<Map<String, Object>> findCallers(
            @PathVariable String projectName,
            @PathVariable String methodName) {

        log.info("Finding callers for method: {} in project: {}", methodName, projectName);
        List<String> callers = callGraphService.findCallers(projectName, methodName);

        Map<String, Object> response = new HashMap<>();
        response.put("projectName", projectName);
        response.put("methodName", methodName);
        response.put("callers", callers);
        response.put("count", callers.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Find all methods that call a given method with pagination and sorting
     *
     * @param projectName Name of the project
     * @param methodName Method being called
     * @param page Page number (0-based, defaults to 0)
     * @param size Page size (defaults to 20)
     * @param sortBy Field to sort by (defaults to "fullMethodPath")
     * @param sortDirection Sort direction (ASC or DESC, defaults to ASC)
     * @return PaginatedResponse containing methods that call the specified method
     */
    @GetMapping("/{projectName}/callers/{methodName}/paginated")
    public ResponseEntity<PaginatedResponse<String>> findCallersPaginated(
            @PathVariable String projectName,
            @PathVariable String methodName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullMethodPath") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("Finding callers for method: {} in project: {} with pagination " +
                "(page: {}, size: {}, sortBy: {}, sortDirection: {})",
                methodName, projectName, page, size, sortBy, sortDirection);

        PaginatedResponse<String> response = callGraphService.findCallersPaginated(
                projectName, methodName, page, size, sortBy, sortDirection);

        return ResponseEntity.ok(response);
    }

    /**
     * Get paginated call graph nodes for a project with sorting
     *
     * @param projectName Name of the project
     * @param page Page number (0-based, defaults to 0)
     * @param size Page size (defaults to 20)
     * @param sortBy Field to sort by (defaults to "fullMethodPath")
     * @param sortDirection Sort direction (ASC or DESC, defaults to ASC)
     * @return PaginatedResponse containing call graph nodes
     */
    @GetMapping("/{projectName}/methods/paginated")
    public ResponseEntity<PaginatedResponse<FullCallGraph>> getMethodsPaginated(
            @PathVariable String projectName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullMethodPath") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("Fetching call graph methods for project: {} with pagination " +
                "(page: {}, size: {}, sortBy: {}, sortDirection: {})",
                projectName, page, size, sortBy, sortDirection);

        PaginatedResponse<FullCallGraph> response = callGraphService.getCallGraphNodesPaginated(
                projectName, page, size, sortBy, sortDirection);

        return ResponseEntity.ok(response);
    }

    /**
     * Get list of all projects with call graphs
     *
     * @return List of project names
     */
    @GetMapping("/projects")
    public ResponseEntity<Map<String, Object>> getAllProjects() {
        log.info("Fetching all projects with call graphs");
        List<String> projects = callGraphService.getAllProjects();

        Map<String, Object> response = new HashMap<>();
        response.put("projects", projects);
        response.put("count", projects.size());

        return ResponseEntity.ok(response);
    }
}
