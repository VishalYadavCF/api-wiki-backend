package com.redcat.tutorials.embedder.dto;

import com.redcat.tutorials.dataloader.model.MethodDetail;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResult {
    private List<String> filePaths;
    private String summary;
    private String contentSummaryId;
}
