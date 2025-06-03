package com.redcat.tutorials.dataloader.model;

import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Model for API method bodies collection
 */
@Document(collection = "api_method_bodies")
@CompoundIndexes({
    @CompoundIndex(name = "projectName_controllerMethod", def = "{'projectName': 1, 'controllerMethod': 1}", unique = false)
})
@Setter
public class ApiMethodBody {

    @Id
    private String id;

    @Indexed
    private String controllerMethod;
    private List<MethodDetail> methods;

    @Indexed
    private String projectName;

    public ApiMethodBody() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getControllerMethod() {
        return controllerMethod;
    }

    public void setControllerMethod(String controllerMethod) {
        this.controllerMethod = controllerMethod;
    }

    public List<MethodDetail> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodDetail> methods) {
        this.methods = methods;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
