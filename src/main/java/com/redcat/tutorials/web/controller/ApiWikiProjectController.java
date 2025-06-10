package com.redcat.tutorials.web.controller;

import com.redcat.tutorials.web.model.GenericResponseDto;
import com.redcat.tutorials.web.model.ProjectDto;
import com.redcat.tutorials.web.service.ApiWikiProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/wiki/projects")
public class ApiWikiProjectController {

    /**
     * Which controller do I need
     * 1. Fetch a list of project
     * 2. Search for a project -> regex / fuzzy search
     * 3. Create project
     */

    private final ApiWikiProjectService apiWikiProjectService;

    public ApiWikiProjectController(ApiWikiProjectService apiWikiProjectService) {
        this.apiWikiProjectService = apiWikiProjectService;
    }

    @GetMapping ("/")
    public ResponseEntity<GenericResponseDto> getProjectList() {
        // Logic to fetch project list
        List<ProjectDto> projectDtos = apiWikiProjectService.getAllProjects();
        GenericResponseDto.builder().data(projectDtos).build();
        return ResponseEntity.ok(GenericResponseDto.builder()
                .data(projectDtos)
                .build());
    }
}
