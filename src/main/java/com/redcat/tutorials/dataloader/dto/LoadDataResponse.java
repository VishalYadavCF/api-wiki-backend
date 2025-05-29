package com.redcat.tutorials.dataloader.dto;

/**
 * Response DTO for data loading operations
 */
public class LoadDataResponse {

    private boolean success;
    private String message;
    private int methodBodiesLoaded;
    private int callGraphNodesLoaded;

    public LoadDataResponse() {
    }

    public LoadDataResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMethodBodiesLoaded() {
        return methodBodiesLoaded;
    }

    public void setMethodBodiesLoaded(int methodBodiesLoaded) {
        this.methodBodiesLoaded = methodBodiesLoaded;
    }

    public int getCallGraphNodesLoaded() {
        return callGraphNodesLoaded;
    }

    public void setCallGraphNodesLoaded(int callGraphNodesLoaded) {
        this.callGraphNodesLoaded = callGraphNodesLoaded;
    }
}
