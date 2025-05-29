package com.redcat.tutorials.dataloader.controller;

import com.redcat.tutorials.dataloader.dto.MethodBodyResponse;
import com.redcat.tutorials.dataloader.dto.PaginatedResponse;
import com.redcat.tutorials.dataloader.exception.ControllerMethodNotFoundException;
import com.redcat.tutorials.dataloader.exception.ProjectNotFoundException;
import com.redcat.tutorials.dataloader.service.MethodBodyService;
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
@RequestMapping("/api/methodbodies")
public class MethodBodyController {

    private static final Logger logger = LoggerFactory.getLogger(MethodBodyController.class);
    private final MethodBodyService methodBodyService;

    @Autowired
    public MethodBodyController(MethodBodyService methodBodyService) {
        this.methodBodyService = methodBodyService;
    }

    /**
     * Get all method bodies for a project
     *
     * @param projectName Name of the project
     * @return List of method body responses
     */
    @GetMapping("/{projectName}")
    public ResponseEntity<Map<String, Object>> getAllMethodBodiesForProject(@PathVariable String projectName) {
        logger.info("REST request to get all method bodies for project: {}", projectName);
        List<MethodBodyResponse> methodBodies = methodBodyService.getAllMethodBodiesForProject(projectName);

        Map<String, Object> response = new HashMap<>();
        response.put("projectName", projectName);
        response.put("methodBodies", methodBodies);
        response.put("count", methodBodies.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all method bodies for a project with pagination and sorting
     *
     * @param projectName Name of the project
     * @param page Page number (0-based, defaults to 0)
     * @param size Page size (defaults to 20)
     * @param sortBy Field to sort by (defaults to "controllerMethod")
     * @param sortDirection Sort direction (ASC or DESC, defaults to ASC)
     * @return PaginatedResponse containing method body responses
     */
    @GetMapping("/{projectName}/paginated")
    public ResponseEntity<PaginatedResponse<MethodBodyResponse>> getAllMethodBodiesForProjectPaginated(
            @PathVariable String projectName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "controllerMethod") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        logger.info("REST request to get paginated method bodies for project: {} " +
                "(page: {}, size: {}, sortBy: {}, sortDirection: {})",
                projectName, page, size, sortBy, sortDirection);

        PaginatedResponse<MethodBodyResponse> response = methodBodyService.getAllMethodBodiesForProjectPaginated(
                projectName, page, size, sortBy, sortDirection);

        return ResponseEntity.ok(response);
    }

    /**
     * Get method body for a specific controller method in a project
     *
     * @param projectName Name of the project
     * @param controllerMethod Controller method path
     * @return Method body response
     */
    @GetMapping("/{projectName}/controllers/{controllerMethod}")
    public ResponseEntity<MethodBodyResponse> getMethodBodyForController(
            @PathVariable String projectName,
            @PathVariable String controllerMethod) {

        logger.info("REST request to get method body for controller: {} in project: {}",
                controllerMethod, projectName);

        MethodBodyResponse methodBody = methodBodyService.getMethodBodyForController(projectName, controllerMethod);
        return ResponseEntity.ok(methodBody);
    }
}
