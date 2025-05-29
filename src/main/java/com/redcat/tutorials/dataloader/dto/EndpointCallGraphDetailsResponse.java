package com.redcat.tutorials.dataloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointCallGraphDetailsResponse {
    private String projectName;
    private String endpointMethod;
    private MethodBodyResponse methodBody;
    private boolean found;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodBodyDetails {
        private String methodName;
        private String body;
        private String filePath;
    }
}

