package com.redcat.tutorials.summariser.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.redcat.tutorials.summariser.model.CodeSummaryContentStatus;
import com.redcat.tutorials.summariser.model.CodeSummaryStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeSummaryContentStatusRepository extends MongoRepository<CodeSummaryContentStatus, String> {

    List<CodeSummaryContentStatus> findBySummaryId(String summaryId);

    List<CodeSummaryContentStatus> findBySummaryIdAndStatus(String summaryId, CodeSummaryStatus status);

    // Find any failed summary content entry
    Optional<CodeSummaryContentStatus> findFirstByStatus(CodeSummaryStatus status);

    // Find any failed summary content entry for a specific project
    Optional<CodeSummaryContentStatus> findFirstByProjectNameAndStatus(String projectName, CodeSummaryStatus status);

    long countBySummaryId(String summaryId);

    long countBySummaryIdAndStatus(String summaryId, CodeSummaryStatus status);
}
