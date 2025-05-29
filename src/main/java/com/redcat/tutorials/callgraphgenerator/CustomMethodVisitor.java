package com.redcat.tutorials.callgraphgenerator;

import org.objectweb.asm.MethodVisitor;

public class CustomMethodVisitor extends MethodVisitor {

    private final String callerMethod;
    private final CallGraph callGraph;

    public CustomMethodVisitor(int api, String name, String descriptor, CallGraph callGraph) {
        super(api);
        this.callGraph = callGraph;
        this.callerMethod = null; // Will be set later by the wrapper
    }

    public CustomMethodVisitor(int api, String owner, String name, String descriptor, CallGraph callGraph) {
        super(api);
        this.callGraph = callGraph;
        this.callerMethod = owner.replace('/', '.') + "." + name;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        String callee = owner.replace('/', '.') + "." + name;
        if (callerMethod != null && !callee.startsWith("java.")) {
            callGraph.addEdge(callerMethod, callee);
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
