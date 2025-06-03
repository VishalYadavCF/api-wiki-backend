package com.redcat.tutorials.summariser.repository;

import com.redcat.tutorials.summariser.model.CodeSummaryStatusEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeSummaryStatusRepo extends MongoRepository<CodeSummaryStatusEntity, String> {
}
