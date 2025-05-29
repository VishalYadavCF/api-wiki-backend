package com.redcat.tutorials.dataloader.dto;

/**
 * Request DTO for loading data from JSON files into MongoDB
 */
public class LoadDataRequest {

    private String directoryPath;
    private String projectName;

    public LoadDataRequest() {
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
