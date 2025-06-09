package com.redcat.tutorials.embedder.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class SearchResponse {
    private List<SearchResult> results;


}
