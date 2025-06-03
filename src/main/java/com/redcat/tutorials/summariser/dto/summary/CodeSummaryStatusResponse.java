package com.redcat.tutorials.summariser.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CodeSummaryStatusResponse {
    private String id;
    private String projectName;
    private List<Map<String, String>> apiEndpointsFinished;
    private Integer totalApiEndpoints;
    private String status;
    private Boolean success;
}
