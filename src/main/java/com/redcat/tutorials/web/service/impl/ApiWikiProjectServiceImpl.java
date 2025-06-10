package com.redcat.tutorials.web.service.impl;

import com.redcat.tutorials.dataloader.repository.ApiMethodBodyRepository;
import com.redcat.tutorials.summariser.model.CodeSummaryContentEntity;
import com.redcat.tutorials.summariser.model.CodeSummaryContentStatus;
import com.redcat.tutorials.summariser.model.CodeSummaryStatus;
import com.redcat.tutorials.summariser.repository.CodeSummaryContentRepository;
import com.redcat.tutorials.summariser.repository.CodeSummaryContentStatusRepository;
import com.redcat.tutorials.web.model.CodeSummaryResponseDto;
import com.redcat.tutorials.web.model.ProjectDto;
import com.redcat.tutorials.web.service.ApiWikiProjectService;
import org.springframework.stereotype.Service;
import com.redcat.tutorials.dataloader.model.ApiMethodBody;

import java.util.Map;
import java.util.stream.Collectors;

import java.util.List;

@Service
public class ApiWikiProjectServiceImpl implements ApiWikiProjectService {

    private final ApiMethodBodyRepository apiMethodBodyRepository;

    private final CodeSummaryContentStatusRepository codeSummaryContentStatusRepository;

    private final CodeSummaryContentRepository codeSummaryContentRepository;

    public ApiWikiProjectServiceImpl(ApiMethodBodyRepository apiMethodBodyRepository,
                                     CodeSummaryContentStatusRepository codeSummaryContentStatusRepository,
                                     CodeSummaryContentRepository codeSummaryContentRepository) {
        this.apiMethodBodyRepository = apiMethodBodyRepository;
        this.codeSummaryContentStatusRepository = codeSummaryContentStatusRepository;
        this.codeSummaryContentRepository = codeSummaryContentRepository;
    }

    @Override
    public List<ProjectDto> getAllProjects() {
        // Fetch all ApiMethodBody entries
        List<ApiMethodBody> allApiMethods = apiMethodBodyRepository.findAllWithoutMethods();
        // Group by projectName and map to ProjectDto
        return allApiMethods.stream()
                .collect(Collectors.groupingBy(ApiMethodBody::getProjectName))
                .entrySet().stream()
                .map(entry -> ProjectDto.builder()
                        .name(entry.getKey())
                        .description("") // No description in model
                        .owner("") // No owner in model
                        .createdAt("") // No createdAt in model
                        .updatedAt("") // No updatedAt in model
                        .gitUrl("") // No gitUrl in model
                        .totalApis(entry.getValue().size())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CodeSummaryResponseDto> getAllControllerSummariesForAProject(String projectName) {
        // Fetch all ApiMethodBody entries
        List<CodeSummaryContentStatus> allApiMethods = codeSummaryContentStatusRepository.findByProjectNameAndStatus(projectName, CodeSummaryStatus.FINISHED);
        List<String> ids = allApiMethods.stream()
                .map(CodeSummaryContentStatus::getId)
                .collect(Collectors.toList());
        Map<String, CodeSummaryContentStatus> summaryStatusMap = allApiMethods.stream()
                .collect(Collectors.toMap(CodeSummaryContentStatus::getId, status -> status));
        List<CodeSummaryContentEntity> codeSummaryContentEntities = codeSummaryContentRepository.findAllByCodeSummaryContentIdIn(ids);

        List<CodeSummaryResponseDto> codeSummaryResponseDtos = codeSummaryContentEntities.stream()
                .map(entity -> CodeSummaryResponseDto.builder()
                        .id(entity.getCodeSummaryContentId())
                        .project(projectName)
                        .controllerName(summaryStatusMap.get(entity.getCodeSummaryContentId()).getControllerMethod())
                        .build())
                .collect(Collectors.toList());
        // Map to ApiSummaryDto
        return codeSummaryResponseDtos;
    }

    @Override
    public CodeSummaryResponseDto getContentById(String codeSummaryContentId) {
        return codeSummaryContentRepository.findByCodeSummaryContentId(codeSummaryContentId)
                .map(entity -> CodeSummaryResponseDto.builder()
                        .id(entity.getCodeSummaryContentId())
                        .content(entity.getSummary())
                        .build())
                .orElse(null);
    }
}
