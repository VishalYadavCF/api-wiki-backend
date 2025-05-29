package com.redcat.tutorials.dataloader.exception;

public class MethodNotFoundException extends RuntimeException {

    public MethodNotFoundException(String methodName, String projectName) {
        super("Method not found: " + methodName + " in project: " + projectName);
    }
}
