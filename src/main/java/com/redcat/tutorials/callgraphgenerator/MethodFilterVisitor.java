package com.redcat.tutorials.callgraphgenerator;

import ddtrot.org.objectweb.asm.Opcodes;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

public class MethodFilterVisitor extends ClassVisitor {
    private boolean isInterface;
    private boolean containsService;
    private boolean hasDataAnnotation;
    private boolean hasGetterAnnotation;
    private boolean hasSetterAnnotation;
    private boolean hasBuilderAnnotation;
    private String currentClassName;

    protected MethodFilterVisitor(int api) {
        super(api);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        this.currentClassName = name.replace('/', '.');
        this.containsService = currentClassName.toLowerCase().contains("service");
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        // Check for Lombok annotations that generate getters/setters/builders
        if (descriptor.contains("Data") || descriptor.endsWith("/Data;")) {
            hasDataAnnotation = true;
        } else if (descriptor.contains("Getter") || descriptor.endsWith("/Getter;")) {
            hasGetterAnnotation = true;
        } else if (descriptor.contains("Setter") || descriptor.endsWith("/Setter;")) {
            hasSetterAnnotation = true;
        } else if (descriptor.contains("Builder") || descriptor.endsWith("/Builder;")) {
            hasBuilderAnnotation = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    public boolean shouldSkipMethodBodyExtraction(String methodName, String methodDescriptor) {
        // Skip if class contains "Service" and is an interface
        if (containsService && isInterface) {
            return true;
        }

        // Skip getters, setters, and builders for annotated classes
        if (hasDataAnnotation || hasGetterAnnotation || hasSetterAnnotation || hasBuilderAnnotation) {
            if (isGetterMethod(methodName, methodDescriptor) ||
                    isSetterMethod(methodName, methodDescriptor) ||
                    isBuilderMethod(methodName, methodDescriptor)) {
                return true;
            }
        }

        return false;
    }

    private boolean isGetterMethod(String methodName, String descriptor) {
        // Typical getter patterns: getName(), isActive(), etc.
        return (methodName.startsWith("get") && methodName.length() > 3 &&
                Character.isUpperCase(methodName.charAt(3)) &&
                !descriptor.startsWith("()V")) || // not void return
                (methodName.startsWith("is") && methodName.length() > 2 &&
                        Character.isUpperCase(methodName.charAt(2)) &&
                        descriptor.startsWith("()Z")); // boolean return
    }

    private boolean isSetterMethod(String methodName, String descriptor) {
        // Typical setter pattern: setName(String name)
        return methodName.startsWith("set") &&
                methodName.length() > 3 &&
                Character.isUpperCase(methodName.charAt(3)) &&
                descriptor.endsWith(")V"); // void return
    }

    private boolean isBuilderMethod(String methodName, String descriptor) {
        // Builder methods typically return the same type or Builder type
        // This is a simplified check - you might need to enhance based on your patterns
        return methodName.equals("builder") ||
                methodName.equals("toBuilder") ||
                (descriptor.contains("Builder") && !descriptor.endsWith(")V"));
    }

    public boolean isInterface() { return isInterface; }
    public boolean containsService() { return containsService; }
    public String getCurrentClassName() { return currentClassName; }
}