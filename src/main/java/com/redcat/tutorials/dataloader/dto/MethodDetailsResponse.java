package com.redcat.tutorials.dataloader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MethodDetailsResponse {
    private String methodName;
    private String filePath;
    private String methodBody;
}
