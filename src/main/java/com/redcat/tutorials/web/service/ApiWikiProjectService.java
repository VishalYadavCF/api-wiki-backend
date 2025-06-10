package com.redcat.tutorials.web.service;

import com.redcat.tutorials.web.model.ApiSummaryDto;
import com.redcat.tutorials.web.model.CodeSummaryResponseDto;
import com.redcat.tutorials.web.model.ProjectDto;

import java.util.List;

public interface ApiWikiProjectService {
    List<ProjectDto> getAllProjects();
    List<CodeSummaryResponseDto>  getAllControllerSummariesForAProject(String projectName);
    CodeSummaryResponseDto getContentById(String codeSummaryContentId);
}
