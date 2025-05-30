package com.redcat.tutorials.callgraphgenerator;

import java.util.*;

public class CallGraph {
    private final Map<String, Set<String>> graph = new HashMap<>();

    public void addEdge(String caller, String callee) {
        graph.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
    }

    public Set<String> getCallees(String method) {
        return graph.getOrDefault(method, Collections.emptySet());
    }

    public Set<String> getAllMethods() {
        return graph.keySet();
    }

    public Map<String, Set<String>> getGraph() {
        return graph;
    }

    public Map<String, Set<String>> getSubGraphFrom(String entryMethod) {
        Map<String, Set<String>> subGraph = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(entryMethod);

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!visited.contains(current)) {
                visited.add(current);
                Set<String> callees = getCallees(current);
                subGraph.put(current, new HashSet<>(callees));
                stack.addAll(callees);
            }
        }

        return subGraph;
    }
}

