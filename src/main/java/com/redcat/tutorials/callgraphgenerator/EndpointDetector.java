package com.redcat.tutorials.callgraphgenerator;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class EndpointDetector {

    private final Map<String, Endpoint> endpoints = new HashMap<>();
    private String currentClass;
    private boolean isRestController;

    public void visitClass(String className) {
        this.currentClass = className;
        this.isRestController = false;
    }

    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM9) {
            boolean isMapped = false;
            String httpMethod = "UNKNOWN";
            String path = "";

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                String annotation = desc.replace("/", ".").replace(";", "");

                if (annotation.endsWith("RestController") || annotation.endsWith("Controller")) {
                    isRestController = true;
                }

                if (annotation.contains("RequestMapping") || annotation.contains("GetMapping") ||
                        annotation.contains("PostMapping") || annotation.contains("PutMapping") ||
                        annotation.contains("DeleteMapping")) {
                    isMapped = true;
                    if (annotation.contains("Get")) httpMethod = "GET";
                    else if (annotation.contains("Post")) httpMethod = "POST";
                    else if (annotation.contains("Put")) httpMethod = "PUT";
                    else if (annotation.contains("Delete")) httpMethod = "DELETE";
                    else httpMethod = "REQUEST";

                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String name, Object value) {
                            if ("value".equals(name) || "path".equals(name)) {
                                path = value.toString();
                            }
                        }
                    };
                }
                return null;
            }

            @Override
            public void visitEnd() {
                if (isRestController && isMapped) {
                    String fqMethod = currentClass + "." + name;
                    endpoints.put(httpMethod + " " + path, new Endpoint(httpMethod, path, fqMethod));
                }
            }
        };
    }

    public Collection<Endpoint> getEndpoints() {
        return endpoints.values();
    }

    public static class Endpoint {
        public final String method;
        public final String path;
        public final String entryMethod;

        public Endpoint(String method, String path, String entryMethod) {
            this.method = method;
            this.path = path;
            this.entryMethod = entryMethod;
        }
    }
}

