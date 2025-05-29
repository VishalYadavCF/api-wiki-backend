package com.redcat.tutorials.dataloader.controller;

import com.redcat.tutorials.dataloader.dto.LoadDataRequest;
import com.redcat.tutorials.dataloader.dto.LoadDataResponse;
import com.redcat.tutorials.dataloader.service.DataLoaderService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for data loading operations
 */
@RestController
@RequestMapping("/api/data")
@Slf4j
public class DataLoaderController {

    private static final Logger logger = LoggerFactory.getLogger(DataLoaderController.class);
    private final DataLoaderService dataLoaderService;

    @Autowired
    public DataLoaderController(DataLoaderService dataLoaderService) {
        this.dataLoaderService = dataLoaderService;
    }

    /**
     * Endpoint to load data from JSON files into MongoDB
     *
     * @param request LoadDataRequest containing directory path and project name
     * @return Response with load status and statistics
     */
    @PostMapping("/load")
    public ResponseEntity<LoadDataResponse> loadData(@RequestBody LoadDataRequest request) {
        log.info("Loading data from directory: {}", request.getDirectoryPath());
        if (request.getDirectoryPath() == null || request.getDirectoryPath().isEmpty()) {
            LoadDataResponse response = new LoadDataResponse(false, "Directory path is required");
            return ResponseEntity.badRequest().body(response);
        }

        if (request.getProjectName() == null || request.getProjectName().isEmpty()) {
            LoadDataResponse response = new LoadDataResponse(false, "Project name is required");
            return ResponseEntity.badRequest().body(response);
        }

        LoadDataResponse response = dataLoaderService.loadDataFromJsonFiles(
            request.getDirectoryPath(),
            request.getProjectName()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
