package com.redcat.tutorials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

public class EndpointCallGraphBuilder {

    private final CallGraph globalGraph;
    private final ObjectMapper objectMapper;
    private String outputDir = "./output"; // Default output directory
    private MethodBodyExtractor methodBodyExtractor; // Add method body extractor
    private boolean extractMethodBodies = false; // Flag to control method body extraction
    private String projectSrcPath; // Source folder path for relative path calculation

    public EndpointCallGraphBuilder(CallGraph globalGraph) {
        this.globalGraph = globalGraph;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Set the output directory for generated files
     * @param outputDir the directory to write output files to
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Enable method body extraction functionality
     * @param classesDirPath the path to the classes directory
     */
    public void enableMethodBodyExtraction(String classesDirPath) {
        try {
            this.methodBodyExtractor = new MethodBodyExtractor(classesDirPath);
            // Try to determine project src path for relative path calculation
            File classesDir = new File(classesDirPath);
            File projectDir = classesDir.getParentFile(); // target folder
            if (projectDir != null) {
                projectDir = projectDir.getParentFile(); // project root
                if (projectDir != null) {
                    File srcDir = new File(projectDir, "src");
                    if (srcDir.exists() && srcDir.isDirectory()) {
                        this.projectSrcPath = srcDir.getAbsolutePath();
                        System.out.println("Using project src path: " + this.projectSrcPath);
                    }
                }
            }

            this.methodBodyExtractor.loadAllClasses();
            this.extractMethodBodies = true;
            System.out.println("Method body extraction enabled");
        } catch (IOException e) {
            System.err.println("Failed to initialize method body extractor: " + e.getMessage());
            this.extractMethodBodies = false;
        }
    }

    public void generateEndpointCallGraphs(Iterable<EndpointDetector.Endpoint> endpoints) {
        // Create output directory if it doesn't exist
        File outDir = new File(outputDir);
        if (!outDir.exists() && !outDir.mkdirs()) {
            System.err.println("Failed to create output directory: " + outputDir);
            return;
        }

        // Also generate the full graph
        try {
            objectMapper.writeValue(new File(outDir, "full_call_graph.json"), globalGraph.getGraph());
            System.out.println("✅ Wrote full call graph to " + new File(outDir, "full_call_graph.json").getPath());
        } catch (IOException e) {
            System.err.println("❌ Failed to write full call graph: " + e.getMessage());
        }

        // Generate individual endpoint call graphs
        int endpointCount = 0;
        for (EndpointDetector.Endpoint endpoint : endpoints) {
            endpointCount++;
            Map<String, Set<String>> subGraph = globalGraph.getSubGraphFrom(endpoint.entryMethod);
            String filename = safeFilename(endpoint.method + "_" + endpoint.path + ".json");
            try {
                objectMapper.writeValue(new File(outDir, filename), subGraph);
                System.out.println("✅ Wrote call graph for " + endpoint.method + " " + endpoint.path + " to " + new File(outDir, filename).getPath());

                // Generate method bodies file if enabled
                if (extractMethodBodies && methodBodyExtractor != null) {
                    generateMethodBodiesJsonFile(endpoint, subGraph);
                }
            } catch (IOException e) {
                System.err.println("❌ Failed to write call graph for " + endpoint.path + ": " + e.getMessage());
            }
        }

        // Generate method bodies for controller methods that aren't mapped to endpoints
        if (extractMethodBodies && methodBodyExtractor != null) {
            try {
                generateControllerMethodBodiesJson();
            } catch (IOException e) {
                System.err.println("❌ Failed to write controller method bodies: " + e.getMessage());
            }
        }

        System.out.println("✨ Generated call graphs for " + endpointCount + " endpoints");
    }

    /**
     * Generate a JSON file containing method bodies for an endpoint's call hierarchy
     */
    private void generateMethodBodiesJsonFile(EndpointDetector.Endpoint endpoint, Map<String, Set<String>> subGraph) throws IOException {
        String methodBodiesFilename = safeFilename(endpoint.method + "_" + endpoint.path + "_method_bodies.json");
        File methodBodiesFile = new File(outputDir, methodBodiesFilename);

        Map<String, MethodBodyExtractor.MethodInfo> methodInfoMap = methodBodyExtractor.extractMethodHierarchyWithInfo(globalGraph, endpoint.entryMethod);

        // Create JSON structure
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("endpoint", endpoint.method + " " + endpoint.path);
        rootNode.put("entryPoint", endpoint.entryMethod);

        ArrayNode methodsArray = rootNode.putArray("methods");

        for (Map.Entry<String, MethodBodyExtractor.MethodInfo> entry : methodInfoMap.entrySet()) {
            String methodName = entry.getKey();
            MethodBodyExtractor.MethodInfo methodInfo = entry.getValue();

            ObjectNode methodNode = objectMapper.createObjectNode();
            methodNode.put("name", methodName);
            methodNode.put("body", methodInfo.methodBody);

            // Add relative path if available
            if (methodInfo.filePath != null) {
                String relativePath = methodInfo.filePath;
                if (projectSrcPath != null && relativePath.startsWith(projectSrcPath)) {
                    relativePath = relativePath.substring(projectSrcPath.length());
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }
                }
                methodNode.put("filePath", relativePath);
            }

            methodsArray.add(methodNode);
        }

        // Write JSON to file
        objectMapper.writeValue(methodBodiesFile, rootNode);

        System.out.println("✅ Wrote method bodies (JSON) for " + endpoint.method + " " + endpoint.path +
            " to " + methodBodiesFile.getPath() + " (" + methodInfoMap.size() + " methods)");
    }

    /**
     * Generate JSON file with method bodies for controller methods that aren't mapped to endpoints
     */
    private void generateControllerMethodBodiesJson() throws IOException {
        File controllerMethodsFile = new File(outputDir, "controller_method_bodies.json");

        // Create root JSON structure
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode controllersArray = rootNode.putArray("controllers");

        for (MethodBodyExtractor.ControllerMethod controllerMethod : methodBodyExtractor.findControllerMethods()) {
            Map<String, MethodBodyExtractor.MethodInfo> methodInfoMap =
                methodBodyExtractor.extractMethodHierarchyWithInfo(globalGraph, controllerMethod.fullName);

            ObjectNode controllerNode = objectMapper.createObjectNode();
            controllerNode.put("controllerMethod", controllerMethod.fullName);
            ArrayNode methodsArray = controllerNode.putArray("methods");

            for (Map.Entry<String, MethodBodyExtractor.MethodInfo> entry : methodInfoMap.entrySet()) {
                String methodName = entry.getKey();
                MethodBodyExtractor.MethodInfo methodInfo = entry.getValue();

                ObjectNode methodNode = objectMapper.createObjectNode();
                methodNode.put("name", methodName);
                methodNode.put("body", methodInfo.methodBody);

                // Add relative path if available
                if (methodInfo.filePath != null) {
                    String relativePath = methodInfo.filePath;
                    if (projectSrcPath != null && relativePath.startsWith(projectSrcPath)) {
                        relativePath = relativePath.substring(projectSrcPath.length());
                        if (relativePath.startsWith("/")) {
                            relativePath = relativePath.substring(1);
                        }
                    }
                    methodNode.put("filePath", relativePath);
                }

                methodsArray.add(methodNode);
            }

            controllersArray.add(controllerNode);
        }

        // Write JSON to file
        objectMapper.writeValue(controllerMethodsFile, rootNode);

        System.out.println("✅ Wrote controller method bodies (JSON) to " + controllerMethodsFile.getPath());
    }

    private String safeFilename(String input) {
        return input.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}

