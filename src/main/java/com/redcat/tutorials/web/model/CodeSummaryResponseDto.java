package com.redcat.tutorials.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CodeSummaryResponseDto {
    private String id;
    private String content;
    private String project;
    private String codeSummaryStatusId;
    private String controllerName;
}
