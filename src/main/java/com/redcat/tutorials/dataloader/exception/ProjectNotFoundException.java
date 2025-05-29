package com.redcat.tutorials.dataloader.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String projectName) {
        super("Project not found with name: " + projectName);
    }
}
