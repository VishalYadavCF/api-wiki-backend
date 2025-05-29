package com.redcat.tutorials.dataloader.model;

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
public class ApiMethodBody {

    @Id
    private String id;

    @Indexed
    private String controllerMethod;
    private List<MethodDetail> methods;

    @Indexed
    private String projectName;

    public static class MethodDetail {
        private String name;
        private String body;
        private String filePath;

        public MethodDetail() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

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
