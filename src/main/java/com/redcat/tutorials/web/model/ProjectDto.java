package com.redcat.tutorials.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectDto {
    private String name;
    private String description;
    private String owner;
    private String createdAt;
    private String updatedAt;
    private String gitUrl;
    private int totalApis;
}
