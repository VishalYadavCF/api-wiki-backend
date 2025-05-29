package com.redcat.tutorials.dataloader.exception;

public class ControllerMethodNotFoundException extends RuntimeException {

    public ControllerMethodNotFoundException(String controllerMethod, String projectName) {
        super("Controller method not found: " + controllerMethod + " in project: " + projectName);
    }
}
