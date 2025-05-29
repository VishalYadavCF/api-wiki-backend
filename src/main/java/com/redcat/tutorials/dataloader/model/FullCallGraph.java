package com.redcat.tutorials.dataloader.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Model for full call graph collection
 */
@Document(collection = "full_call_graph")
@CompoundIndexes({
    @CompoundIndex(name = "projectName_fullMethodPath", def = "{'projectName': 1, 'fullMethodPath': 1}", unique = true)
})
public class FullCallGraph {

    @Id
    private String id;

    @Indexed
    private String projectName;

    @Indexed
    private String fullMethodPath;

    private List<String> childMethods;

    public FullCallGraph() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getFullMethodPath() {
        return fullMethodPath;
    }

    public void setFullMethodPath(String fullMethodPath) {
        this.fullMethodPath = fullMethodPath;
    }

    public List<String> getChildMethods() {
        return childMethods;
    }

    public void setChildMethods(List<String> childMethods) {
        this.childMethods = childMethods;
    }
}
