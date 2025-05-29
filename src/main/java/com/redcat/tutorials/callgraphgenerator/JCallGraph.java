package com.redcat.tutorials.callgraphgenerator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class JCallGraph {

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java com.redcat.tutorials.callgraphgenerator.JCallGraph <classes_directory> [output_directory]");
            System.exit(1);
        }

        // Process arguments
        String classesPath = args[0];
        String outputDir = args.length > 1 ? args[1] : "./output";

        File classesDir = new File(classesPath);
        if (!classesDir.isDirectory()) {
            System.err.println("Provided classes path is not a directory: " + classesPath);
            System.exit(1);
        }

        System.out.println("Reading classes from: " + classesDir.getAbsolutePath());
        System.out.println("Output will be written to: " + outputDir);

        Set<String> classFiles = new HashSet<>();
        // Pass project root to collectClassFiles
        File projectRoot = new File("").getAbsoluteFile();
        collectClassFiles(classesDir, classFiles, projectRoot);
        System.out.println("Found " + classFiles.size() + " class files to analyze");

        DynamicCallManager dynamicCallManager = new DynamicCallManager();
        CallGraph globalCallGraph = new CallGraph();
        EndpointDetector endpointDetector = new EndpointDetector();

        for (String classFile : classFiles) {
            try (FileInputStream in = new FileInputStream(classFile)) {
                ClassReader reader = new ClassReader(in);
                ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        String className = name.replace('/', '.');
                        endpointDetector.visitClass(className);
                    }

                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return endpointDetector.visitMethod(access, name, descriptor, signature, exceptions);
                    }
                };
                reader.accept(visitor, ClassReader.EXPAND_FRAMES);

                // Build full method-level call graph
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    private String className;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        this.className = name;
                    }

                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return new CustomMethodVisitor(Opcodes.ASM9, className, name, descriptor, globalCallGraph);
                    }
                }, ClassReader.EXPAND_FRAMES);
            }
        }

        Collection<EndpointDetector.Endpoint> endpoints = endpointDetector.getEndpoints();
        System.out.println("Detected " + endpoints.size() + " REST endpoints");

        EndpointCallGraphBuilder builder = new EndpointCallGraphBuilder(globalCallGraph);
        builder.setOutputDir(outputDir);
        // Pass outputDir as projectRoot for relative path calculation
        projectRoot = new File(outputDir).getAbsoluteFile();
        // Re-collect class files with outputDir as project root if needed
        // collectClassFiles(classesDir, classFiles, projectRoot);

        // Enable method body extraction feature
        try {
            System.out.println("Enabling method body extraction...");
            builder.enableMethodBodyExtraction(classesPath);
        } catch (Exception e) {
            System.err.println("Failed to enable method body extraction: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Continuing without method body extraction");
        }

        builder.generateEndpointCallGraphs(endpoints);

        System.out.println("Analysis complete! Results available in: " + outputDir);
    }

    private static void collectClassFiles(File dir, Set<String> classFiles, File projectRoot) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectClassFiles(file, classFiles, projectRoot);
                } else if (file.getName().endsWith(".class")) {
                    // Add relative path instead of absolute path
                    String relativePath = projectRoot.toPath().relativize(file.getAbsoluteFile().toPath()).toString();
                    classFiles.add(relativePath);
                }
            }
        }
    }
}
