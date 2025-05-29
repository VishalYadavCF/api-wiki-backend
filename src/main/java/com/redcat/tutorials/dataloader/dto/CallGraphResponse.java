package com.redcat.tutorials.dataloader.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for call graph data
 */
public class CallGraphResponse {

    private String projectName;
    private String methodName;
    private List<String> directCalls;
    private Map<String, List<String>> fullCallGraph;
    private boolean found;
    private String message;

    public CallGraphResponse() {
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<String> getDirectCalls() {
        return directCalls;
    }

    public void setDirectCalls(List<String> directCalls) {
        this.directCalls = directCalls;
    }

    public Map<String, List<String>> getFullCallGraph() {
        return fullCallGraph;
    }

    public void setFullCallGraph(Map<String, List<String>> fullCallGraph) {
        this.fullCallGraph = fullCallGraph;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
