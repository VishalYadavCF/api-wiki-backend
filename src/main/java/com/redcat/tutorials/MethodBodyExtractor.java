package com.redcat.tutorials;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts method bodies from class files and organizes them by call hierarchy
 */
public class MethodBodyExtractor {

    private final File classesDir;
    private final Map<String, String> methodBodies = new HashMap<>();
    private final Map<String, MethodInfo> methodInfoMap = new HashMap<>();
    private final Map<String, String> classPathMap = new HashMap<>();
    private final Map<String, String> sourceFileMap = new HashMap<>();

    public MethodBodyExtractor(String classesDirPath) {
        this.classesDir = new File(classesDirPath);
    }

    /**
     * Load all class files and extract method bodies
     */
    public void loadAllClasses() throws IOException {
        Set<String> classFiles = new HashSet<>();
        collectClassFiles(classesDir, classFiles);

        for (String classFile : classFiles) {
            try (FileInputStream in = new FileInputStream(classFile)) {
                ClassReader reader = new ClassReader(in);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);

                String className = classNode.name.replace('/', '.');

                // Store the path to this class file
                classPathMap.put(className, classFile);

                // Get source file information if available
                if (classNode.sourceFile != null) {
                    sourceFileMap.put(className, classNode.sourceFile);
                }

                for (MethodNode methodNode : classNode.methods) {
                    String methodName = className + "." + methodNode.name;

                    // Extract method body using ASM Textifier
                    Textifier textifier = new Textifier();
                    MethodVisitor visitor = new TraceMethodVisitor(textifier);
                    methodNode.accept(visitor);

                    StringWriter sw = new StringWriter();
                    textifier.print(new PrintWriter(sw));
                    String methodBody = sw.toString();

                    // Store method body
                    methodBodies.put(methodName, methodBody);

                    // Find and read source file if possible for better method body extraction
                    String sourceBody = tryToReadSourceMethod(className, methodNode.name);

                    // Create method info with source info if available
                    MethodInfo methodInfo = new MethodInfo(
                            methodName,
                            methodNode.name,
                            methodNode.desc,
                            className,
                            methodNode.access,
                            classFile,
                            sourceBody != null ? sourceBody : methodBody
                    );

                    methodInfoMap.put(methodName, methodInfo);
                }
            } catch (Exception e) {
                System.err.println("Error processing class file: " + classFile);
                e.printStackTrace();
            }
        }

        System.out.println("Loaded " + methodBodies.size() + " method bodies from " + classFiles.size() + " class files");
    }

    /**
     * Try to read the actual source code for a method
     * This attempts to find the corresponding .java file by checking common locations
     */
    private String tryToReadSourceMethod(String className, String methodName) {
        try {
            // Try to find source file based on className
            String classFilePath = classPathMap.get(className);
            if (classFilePath == null) return null;

            File classFile = new File(classFilePath);
            String projectRoot = findProjectRoot(classFile);
            if (projectRoot == null) return null;

            // Build potential source paths
            List<String> potentialSourcePaths = new ArrayList<>();

            // Convert class name to path format
            String classPath = className.replace('.', '/');

            // Try src/main/java location
            potentialSourcePaths.add(projectRoot + "/src/main/java/" + classPath + ".java");

            // Try src location
            potentialSourcePaths.add(projectRoot + "/src/" + classPath + ".java");

            // Try looking for source file in package structure that matches class file structure
            String packagePath = className.substring(0, className.lastIndexOf('.'));
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

            // Use sourceFile information if available
            String sourceFile = sourceFileMap.get(className);
            if (sourceFile == null) {
                sourceFile = simpleClassName + ".java";
            }

            // Search for source file
            for (String potentialPath : potentialSourcePaths) {
                File sourceFile1 = new File(potentialPath);
                if (sourceFile1.exists()) {
                    String source = readJavaSourceFile(sourceFile1.getAbsolutePath());
                    if (source != null) {
                        String methodBody = extractMethodBody(source, methodName);
                        if (methodBody != null) {
                            return methodBody;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in source lookup
        }
        return null;
    }

    // Extracts the body of a method with the given name from the source code using regex
    private String extractMethodBody(String source, String methodName) {
        // This regex matches the method signature and captures the method body (including nested braces)
        // It works for most standard Java methods, but may not handle all edge cases (e.g., inner classes, lambdas)
        String regex = "(?s)(?:public|protected|private|static|\\s)+[\\w<>\\[\\]]+\\s+" + methodName + "\\s*\\([^)]*\\)\\s*(?:throws[^{]*)?\\{((?:[^{}]*+|\\{(?:[^{}]*+|\\{[^{}]*+\\})*+\\})*+)\\}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Find the project root directory by looking for pom.xml or build.gradle
     */
    private String findProjectRoot(File file) {
        File current = file.getParentFile();
        int maxDepth = 10; // Prevent infinite loop

        while (current != null && maxDepth-- > 0) {
            if (new File(current, "pom.xml").exists() ||
                new File(current, "build.gradle").exists()) {
                return current.getAbsolutePath();
            }
            current = current.getParentFile();
        }

        return null;
    }

    /**
     * Read a Java source file and return its contents
     */
    private String readJavaSourceFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Extract method bodies for a controller endpoint and its call hierarchy
     * @param callGraph The call graph
     * @param entryPoint The controller method entry point
     * @return Map of method names to their bodies, ordered by call hierarchy
     */
    public Map<String, String> extractMethodHierarchy(CallGraph callGraph, String entryPoint) {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        List<String> orderedMethods = new ArrayList<>();

        // Use DFS to build ordered list of methods in call hierarchy
        extractMethodsInOrder(callGraph, entryPoint, visited, orderedMethods);

        // Build the result map in order
        for (String method : orderedMethods) {
            if (methodBodies.containsKey(method)) {
                result.put(method, methodBodies.get(method));
            } else {
                // For methods we don't have bodies for (like from external libraries)
                result.put(method, "// Method body not available (external library or JDK method)");
            }
        }

        return result;
    }

    /**
     * Extract method info objects for a controller endpoint and its call hierarchy
     * @param callGraph The call graph
     * @param entryPoint The controller method entry point
     * @return Map of method names to their MethodInfo objects, ordered by call hierarchy
     */
    public Map<String, MethodInfo> extractMethodHierarchyWithInfo(CallGraph callGraph, String entryPoint) {
        Map<String, MethodInfo> result = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        List<String> orderedMethods = new ArrayList<>();

        // Use DFS to build ordered list of methods in call hierarchy
        extractMethodsInOrder(callGraph, entryPoint, visited, orderedMethods);

        // Build the result map in order
        for (String method : orderedMethods) {
            if (methodInfoMap.containsKey(method)) {
                result.put(method, methodInfoMap.get(method));
            } else {
                // For methods we don't have info for (like from external libraries)
                result.put(method, new MethodInfo(
                    method,
                    method.substring(method.lastIndexOf('.') + 1),
                    "()",
                    method.substring(0, method.lastIndexOf('.')),
                    0,
                    null,
                    "// Method body not available (external library or JDK method)"
                ));
            }
        }

        return result;
    }

    /**
     * Recursively extract methods in call order using DFS
     */
    private void extractMethodsInOrder(CallGraph callGraph, String currentMethod, Set<String> visited, List<String> orderedMethods) {
        if (visited.contains(currentMethod)) {
            return;
        }

        visited.add(currentMethod);
        orderedMethods.add(currentMethod);

        Set<String> callees = callGraph.getCallees(currentMethod);
        if (callees != null && !callees.isEmpty()) {
            // Sort callees to ensure consistent ordering
            List<String> sortedCallees = new ArrayList<>(callees);
            Collections.sort(sortedCallees);

            for (String callee : sortedCallees) {
                if (!callee.startsWith("java.") && !callee.startsWith("javax.")) {
                    extractMethodsInOrder(callGraph, callee, visited, orderedMethods);
                }
            }
        }
    }

    /**
     * Find all controller methods in the codebase
     * @return List of controller methods
     */
    public List<ControllerMethod> findControllerMethods() {
        List<ControllerMethod> controllerMethods = new ArrayList<>();

        for (Map.Entry<String, MethodInfo> entry : methodInfoMap.entrySet()) {
            MethodInfo methodInfo = entry.getValue();
            String className = methodInfo.className;

            // Check if this is a controller class (simple heuristic)
            if (className.contains(".controllers.") || className.endsWith("Controller")) {
                controllerMethods.add(new ControllerMethod(
                    methodInfo.fullName,
                    methodInfo.name,
                    className
                ));
            }
        }

        return controllerMethods;
    }

    private static void collectClassFiles(File dir, Set<String> classFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectClassFiles(file, classFiles);
                } else if (file.getName().endsWith(".class")) {
                    classFiles.add(file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Class representing information about a method
     */
    public static class MethodInfo {
        public final String fullName;
        public final String name;
        public final String descriptor;
        public final String className;
        public final int access;
        public final String filePath;
        public final String methodBody;

        public MethodInfo(String fullName, String name, String descriptor, String className, int access,
                        String filePath, String methodBody) {
            this.fullName = fullName;
            this.name = name;
            this.descriptor = descriptor;
            this.className = className;
            this.access = access;
            this.filePath = filePath;
            this.methodBody = methodBody;
        }
    }

    /**
     * Class representing a controller method
     */
    public static class ControllerMethod {
        public final String fullName;
        public final String methodName;
        public final String controllerName;

        public ControllerMethod(String fullName, String methodName, String controllerName) {
            this.fullName = fullName;
            this.methodName = methodName;
            this.controllerName = controllerName;
        }

        @Override
        public String toString() {
            return fullName;
        }
    }
}
