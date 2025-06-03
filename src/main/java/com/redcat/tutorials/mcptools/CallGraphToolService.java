package com.redcat.tutorials.mcptools;

import com.redcat.tutorials.dataloader.dto.CallGraphResponse;
import com.redcat.tutorials.dataloader.dto.MethodBodyResponse;
import com.redcat.tutorials.dataloader.exception.ControllerMethodNotFoundException;
import com.redcat.tutorials.dataloader.exception.MethodNotFoundException;
import com.redcat.tutorials.dataloader.exception.ProjectNotFoundException;
import com.redcat.tutorials.dataloader.model.ApiMethodBody;
import com.redcat.tutorials.dataloader.service.CallGraphService;
import com.redcat.tutorials.dataloader.service.MethodBodyService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI Tool for accessing code call graph information
 */
@Component
@Slf4j
public class CallGraphToolService {

    private final CallGraphService callGraphService;

    private final MethodBodyService methodBodyService;

    @Autowired
    public CallGraphToolService(CallGraphService callGraphService,
                                MethodBodyService methodBodyService) {
        this.callGraphService = callGraphService;
        this.methodBodyService = methodBodyService;
    }

    /**
     * Get the call graph for a specific method in a project
     *
     * @param projectName The project name to search in
     * @param methodName  The full method name to get the call graph for
     * @return The call graph response containing all method calls
     */
    @Tool(name = "getMethodNamesInACallGraph", description = "Retrieve the names of methods involves in a call heirarchy for a specific method in a project")
    public CallGraphResponse getMethodCallGraph(
            @Parameter(description = "The name of the project") String projectName,
            @Parameter(description = "The full method name (e.g., com.example.Service.methodName)") String methodName
    ) {
        try {
            log.info("AI Tool: Retrieving call graph for method {} in project {}", methodName, projectName);
            return callGraphService.getMethodCallGraph(projectName, methodName);
        } catch (ProjectNotFoundException | MethodNotFoundException e) {
            log.warn("AI Tool: Error retrieving call graph", e);
            CallGraphResponse errorResponse = new CallGraphResponse();
            errorResponse.setFound(false);
            errorResponse.setMessage(e.getMessage());
            errorResponse.setProjectName(projectName);
            errorResponse.setMethodName(methodName);
            return errorResponse;
        }
    }

    /**
     * Get the call graph for a specific method in a project
     *
     * @param projectName The project name to search in
     * @return The call graph response containing all method calls
     */
    @Tool(name = "getControllerEndpoints", description = "Retrieve the list of api endpoints/controller methods in the project")
    public List<ApiMethodBody> getAllTheControllerEndpoints(
            @Parameter(description = "The name of the project") String projectName
    ) {
        try {
            log.info("AI Tool: Retrieving call graph for method {} in project {}", projectName);
            return methodBodyService.getAllTheControllerEndpoints(projectName);
        } catch (ProjectNotFoundException | MethodNotFoundException e) {
            log.warn("AI Tool: Error retrieving call graph", e);
//            ApiMethodBody apiMethodBody = new ApiMethodBody();
//            errorResponse.setFound(false);
//            errorResponse.setMessage(e.getMessage());
//            errorResponse.setProjectName(projectName);
            return null;
        }
    }

    /**
     * Find all methods that call a specific method
     *
     * @param projectName The project name to search in
     * @param methodName  The method name to find callers for
     * @return List of caller methods
     */
    @Tool(name = "findCallers", description = "Find all the methods that call a specific method in a project")
    public Map<String, Object> findCallers(
            @Parameter(description = "The name of the project") String projectName,
            @Parameter(description = "The full method name (e.g., com.example.Service.methodName)") String methodName
    ) {
        try {
            log.info("AI Tool: Finding callers for method {} in project {}", methodName, projectName);
            List<String> callers = callGraphService.findCallers(projectName, methodName);

            Map<String, Object> response = new HashMap<>();
            response.put("projectName", projectName);
            response.put("methodName", methodName);
            response.put("callers", callers);
            response.put("found", true);
            response.put("count", callers.size());
            return response;
        } catch (ProjectNotFoundException | MethodNotFoundException e) {
            log.warn("AI Tool: Error finding callers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("projectName", projectName);
            errorResponse.put("methodName", methodName);
            errorResponse.put("found", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("callers", List.of());
            errorResponse.put("count", 0);
            return errorResponse;
        }
    }

    /**
     * List all projects available in the call graph database
     *
     * @return List of project names
     */
    @Tool(name = "listAllProjects", description = "List all projects/codebase/repositories available in the call graph database")
    public Map<String, Object> listAllProjects() {
        log.info("AI Tool: Listing all projects");
        List<String> projects = callGraphService.getAllProjects();

        Map<String, Object> response = new HashMap<>();
        response.put("projects", projects);
        response.put("count", projects.size());
        return response;
    }

    /**
     * Get method body for a specific controller method in a project
     *
     * @param projectName      The project name to search in
     * @param controllerMethod The controller method path
     * @return The method body response with all related methods
     */
    @Tool(description = "Retrieve the method body for a specific controller endpoint in a project, " +
            "showing all the methods it uses along with the entire bodies of all the methods which " +
            "gets called when the API is invoked", name="getApiEndpointOrControllerMethodBody")
    public MethodBodyResponse getApiEndpointOrControllerMethodBody(
            @Parameter(description = "The name of the project") String projectName,
            @Parameter(description = "The controller method path (e.g., com.example.UserController.getUser)") String controllerMethod
    ) {
        try {
            log.info("AI Tool: Retrieving method body for controller {} in project {}",
                    controllerMethod, projectName);

            return methodBodyService.getAllMethodBodiesForCallGraph(projectName, controllerMethod);
        } catch (ProjectNotFoundException | ControllerMethodNotFoundException e) {
            log.warn("AI Tool: Error retrieving method body", e);

            MethodBodyResponse errorResponse = new MethodBodyResponse();
            errorResponse.setFound(false);
            errorResponse.setMessage(e.getMessage());
            errorResponse.setProjectName(projectName);
            errorResponse.setControllerMethod(controllerMethod);

            return errorResponse;
        }
    }

    /**
     * Get all controller method bodies for a project
     *
     * @param projectName The project name to search in
     * @return Map containing all method bodies for the project
     */
    @Tool(description = "Retrieve all controller method bodies for a specific project", name="getAllProjectMethodBodies")
    public Map<String, Object> getAllProjectMethodBodies(
            @Parameter(description = "The name of the project") String projectName
    ) {
        try {
            log.info("AI Tool: Retrieving all method bodies for project {}", projectName);

            List<MethodBodyResponse> methodBodies = methodBodyService.getAllMethodBodiesForProject(projectName);

            Map<String, Object> response = new HashMap<>();
            response.put("projectName", projectName);
            response.put("methodBodies", methodBodies);
            response.put("count", methodBodies.size());
            response.put("found", true);

            return response;
        } catch (ProjectNotFoundException e) {
            log.warn("AI Tool: Error retrieving project method bodies", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("projectName", projectName);
            errorResponse.put("found", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("methodBodies", List.of());
            errorResponse.put("count", 0);

            return errorResponse;
        }
    }

}