package com.redcat.tutorials.dataloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiListResponse {
    private List<MethodDetails> apiMethodList;
    private Boolean found;
    private String projectName;
    private String message;
}
