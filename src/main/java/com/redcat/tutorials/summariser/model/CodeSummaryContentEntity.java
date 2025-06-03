package com.redcat.tutorials.summariser.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "code_summary_content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSummaryContentEntity {

    @Id
    private String id;

    private String codeSummaryContentId;

    private String summary;
}
