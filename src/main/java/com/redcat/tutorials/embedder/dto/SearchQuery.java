package com.redcat.tutorials.embedder.dto;

import lombok.Data;

@Data
public class SearchQuery {
    private String query;
    private int topK;
    private double threshold;
    private String projectName;
}
