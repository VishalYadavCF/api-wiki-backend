package com.redcat.tutorials.summariser.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.redcat.tutorials.summariser.model.CodeSummaryContentEntity;

import java.util.Optional;

@Repository
public interface CodeSummaryContentRepository extends MongoRepository<CodeSummaryContentEntity, String> {

    Optional<CodeSummaryContentEntity> findByCodeSummaryContentId(String codeSummaryContentId);
}
