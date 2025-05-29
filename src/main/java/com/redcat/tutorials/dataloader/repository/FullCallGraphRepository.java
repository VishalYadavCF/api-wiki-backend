package com.redcat.tutorials.dataloader.repository;

import com.redcat.tutorials.dataloader.model.FullCallGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FullCallGraphRepository extends MongoRepository<FullCallGraph, String> {

    /**
     * Find all call graph nodes for a specific project
     *
     * @param projectName the name of the project
     * @return list of call graph nodes
     */
    List<FullCallGraph> findByProjectName(String projectName);

    /**
     * Find all call graph nodes for a specific project with pagination and sorting
     *
     * @param projectName the name of the project
     * @param pageable the pagination information
     * @return paged list of call graph nodes
     */
    Page<FullCallGraph> findByProjectName(String projectName, Pageable pageable);

    /**
     * Find a specific method in a project's call graph
     *
     * @param projectName the name of the project
     * @param fullMethodPath the full method path
     * @return the FullCallGraph if found
     */
    Optional<FullCallGraph> findByProjectNameAndFullMethodPath(String projectName, String fullMethodPath);

    Optional<List<FullCallGraph>> findByProjectNameAndFullMethodPathLike(String projectName, String fullMethodPathPattern);

    /**
     * Check if a project exists in the call graph
     *
     * @param projectName the name of the project
     * @return true if the project exists
     */
    boolean existsByProjectName(String projectName);

    /**
     * Find all methods that have a given method as a child method
     *
     * @param projectName the name of the project
     * @param childMethod the child method to search for
     * @return list of call graph nodes that have the specified child method
     */
    List<FullCallGraph> findByProjectNameAndChildMethodsContaining(String projectName, String childMethod);

    /**
     * Find all methods that have a given method as a child method with pagination and sorting
     *
     * @param projectName the name of the project
     * @param childMethod the child method to search for
     * @param pageable the pagination information
     * @return paged list of call graph nodes that have the specified child method
     */
    Page<FullCallGraph> findByProjectNameAndChildMethodsContaining(String projectName, String childMethod, Pageable pageable);
}
