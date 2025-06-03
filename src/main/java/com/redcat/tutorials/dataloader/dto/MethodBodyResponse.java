package com.redcat.tutorials.dataloader.dto;

import java.util.List;

/**
 * Response DTO for method body information
 */
public class MethodBodyResponse {

    private String projectName;
    private String controllerMethod;
    private List<MethodDetailResponse> methods;
    private boolean found;
    private String message;

    public static class MethodDetailResponse {
        private String name;
        private String body;
        private String filePath;

        public MethodDetailResponse() {
        }

        public MethodDetailResponse(MethodDetailResponse methodDetailResponse) {
            this.name = methodDetailResponse.getName();
            this.body = methodDetailResponse.getBody();
            this.filePath = methodDetailResponse.getFilePath();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

    public MethodBodyResponse() {
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getControllerMethod() {
        return controllerMethod;
    }

    public void setControllerMethod(String controllerMethod) {
        this.controllerMethod = controllerMethod;
    }

    public List<MethodDetailResponse> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodDetailResponse> methods) {
        this.methods = methods;
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
