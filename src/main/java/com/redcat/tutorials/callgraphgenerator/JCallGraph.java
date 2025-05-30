package com.redcat.tutorials.callgraphgenerator;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
public class JCallGraph {

    public static void main(String[] args) throws IOException {

        String classesPath = "/Users/vishal.yadav/IdeaProjects/commonauth/commonauth/target/classes";
        String outputDir = args.length > 1 ? args[1] : "./output";

        File classesDir = new File(classesPath);
        if (!classesDir.isDirectory()) {
            System.err.println("Provided classes path is not a directory: " + classesPath);
            System.exit(1);
        }

        System.out.println("Reading classes from: " + classesDir.getAbsolutePath());
        System.out.println("Output will be written to: " + new File(outputDir).getAbsolutePath());

        // Collect all .class files
        Set<String> classFiles = new HashSet<>();
        collectClassFiles(classesDir, classFiles);
        System.out.println("Found " + classFiles.size() + " class files to analyze");

        // First pass: interface -> implementation mapping
        Map<String, Set<String>> interfaceToImpls = new HashMap<>();
        Map<String, String> classNameToFile = new HashMap<>();
        for (String classFile : classFiles) {
            try (FileInputStream in = new FileInputStream(classFile)) {
                ClassReader reader = new ClassReader(in);
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    String currentClass;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        currentClass = name.replace('/', '.');
                        classNameToFile.put(currentClass, classFile);
                        if (interfaces != null) {
                            for (String iface : interfaces) {
                                String ifaceName = iface.replace('/', '.');
                                interfaceToImpls.computeIfAbsent(ifaceName, k -> new HashSet<>()).add(currentClass);
                            }
                        }
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }

        // Prepare detectors and graph container
        EndpointDetector endpointDetector = new EndpointDetector();
        CallGraph globalCallGraph = new CallGraph();

        // Second pass: detect endpoints and build call graph
        for (String classFile : classFiles) {
            try (FileInputStream in = new FileInputStream(classFile)) {
                ClassReader reader = new ClassReader(in);
                // detect endpoints
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        endpointDetector.visitClass(name.replace('/', '.'));
                    }

                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return endpointDetector.visitMethod(access, name, descriptor, signature, exceptions);
                    }
                }, ClassReader.EXPAND_FRAMES);

                // record calls
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    String currentClass;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        currentClass = name;
                    }

                    @Override
                    public CustomMethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return new CustomMethodVisitor(Opcodes.ASM9, currentClass, name, descriptor, globalCallGraph);
                    }
                }, ClassReader.EXPAND_FRAMES);
            }
        }

        // Expand interface calls to implementations
        Map<String, Set<String>> expanded = new HashMap<>();
        for (String caller : globalCallGraph.getAllMethods()) {
            Set<String> callees = new HashSet<>();
            for (String callee : globalCallGraph.getCallees(caller)) {
                callees.add(callee);
                String className = callee.contains(".") ? callee.substring(0, callee.lastIndexOf('.')) : callee;
                String methodName = callee.contains(".") ? callee.substring(callee.lastIndexOf('.') + 1) : callee;
                if (interfaceToImpls.containsKey(className)) {
                    for (String impl : interfaceToImpls.get(className)) {
                        callees.add(impl + "." + methodName);
                    }
                }
            }
            expanded.put(caller, callees);
        }
        globalCallGraph.getGraph().clear();
        expanded.forEach((c, cl) -> cl.forEach(callee -> globalCallGraph.addEdge(c, callee)));

        // Build and write endpoint call graphs
        EndpointCallGraphBuilder builder = new EndpointCallGraphBuilder(globalCallGraph);
        builder.setOutputDir(outputDir);
        try {
            System.out.println("Enabling method body extraction...");
            builder.enableMethodBodyExtraction(classesPath);
        } catch (Exception e) {
            System.err.println("Failed to enable method body extraction: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Continuing without method body extraction");
        }

        Collection<EndpointDetector.Endpoint> endpoints = endpointDetector.getEndpoints();
        System.out.println("Detected " + endpoints.size() + " REST endpoints");
        builder.generateEndpointCallGraphs(endpoints);
        System.out.println("Analysis complete! Results in: " + outputDir);
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

    private static MethodFilterVisitor analyzeClassForFiltering(String classFile) throws IOException {
        try (FileInputStream in = new FileInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            MethodFilterVisitor filterVisitor = new MethodFilterVisitor(Opcodes.ASM9);
            reader.accept(filterVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            return filterVisitor;
        }
    }
}
