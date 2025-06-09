package com.redcat.tutorials.embedder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class VectorMatch {
    private String id;
    private float score;
}
