package com.redcat.tutorials.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiSummaryDto {
    private String id;
    private String content;
    private String apiTitle;
}
