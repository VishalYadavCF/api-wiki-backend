package com.redcat.tutorials.web.controller;

import com.redcat.tutorials.web.model.GenericResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wiki/projects")
public class ApiWikiProjectController {

    /**
     * Which controller do I need
     * 1. Fetch a list of project
     * 2. Search for a project -> regex / fuzzy search
     * 3. Create project
     */

    @GetMapping ("/")
    public ResponseEntity<GenericResponseDto> getProjectList() {
        // Logic to fetch project list
        return ResponseEntity.ok(GenericResponseDto.builder()
                .data(List.of("commonauth", "pgupisvc", "pgrefundsvc", "streaminganalytics")) // Replace with actual data
                .error(null)
                .success(true)
                .message("Project list fetched successfully")
                .build());
    }
}
