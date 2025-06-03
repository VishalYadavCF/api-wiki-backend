package com.redcat.tutorials.summariser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection="code_summary_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSummaryStatusEntity {

    @Id
    private String id;

    private String projectName;

    private List<Map<String, String>> apiEndpointsFinished;

    private Integer totalApiEndpoints;

    private CodeSummaryStatus status;

    private Boolean success;
}
