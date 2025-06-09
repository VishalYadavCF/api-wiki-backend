package com.redcat.tutorials.dataloader.repository;

import com.redcat.tutorials.dataloader.model.ApiMethodBody;
import com.redcat.tutorials.dataloader.model.FullCallGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiMethodBodyRepository extends MongoRepository<ApiMethodBody, String> {

    /**
     * Find all API method bodies for a specific project
     *
     * @param projectName the name of the project
     * @return list of API method bodies
     */
    List<ApiMethodBody> findByProjectName(String projectName);

    /**
     * Find all API method bodies for a specific project with pagination and sorting
     *
     * @param projectName the name of the project
     * @param pageable the pagination information
     * @return paged list of API method bodies
     */
    Page<ApiMethodBody> findByProjectName(String projectName, Pageable pageable);

    /**
     * Find a specific controller method in a project
     *
     * @param projectName the name of the project
     * @param controllerMethod the controller method path
     * @return the ApiMethodBody if found
     */
    Optional<ApiMethodBody> findByProjectNameAndControllerMethod(String projectName, String controllerMethod);

    /**
     * Check if a project exists
     *
     * @param projectName the name of the project
     * @return true if the project exists
     */
    boolean existsByProjectName(String projectName);

    Optional<List<ApiMethodBody>> findByProjectNameAndControllerMethodLikeIgnoreCase(String projectName, String controllerMethod);

    @Query(value = "{}", fields = "{ 'id': 1, 'controllerMethod': 1, 'projectName': 1 }")
    List<ApiMethodBody> findAllWithoutMethods();
}
