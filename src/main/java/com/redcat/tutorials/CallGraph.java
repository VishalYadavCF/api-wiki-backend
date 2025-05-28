package com.redcat.tutorials;

import java.util.*;

public class CallGraph {
    private final Map<String, Set<String>> graph = new HashMap<>();

    /**
     * Add an edge from caller to callee in the call graph
     * @param caller the calling method
     * @param callee the called method
     */
    public void addEdge(String caller, String callee) {
        graph.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
    }

    /**
     * Get all methods called by the given method
     * @param method the method to get callees for
     * @return set of called methods, or empty set if none
     */
    public Set<String> getCallees(String method) {
        return graph.getOrDefault(method, Collections.emptySet());
    }

    /**
     * Get all methods that call the given method
     * @param method the method to get callers for
     * @return set of calling methods
     */
    public Set<String> getCallers(String method) {
        Set<String> callers = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getValue().contains(method)) {
                callers.add(entry.getKey());
            }
        }
        return callers;
    }

    /**
     * Get all entries in the graph
     * @return a map of caller methods to their called methods
     */
    public Map<String, Set<String>> getGraph() {
        return Collections.unmodifiableMap(graph);
    }

    /**
     * Gets a sub-graph starting from the given entry point
     * @param entryPoint the starting method
     * @return a map containing the reachable call graph from the entry point
     */
    public Map<String, Set<String>> getSubGraphFrom(String entryPoint) {
        Map<String, Set<String>> subGraph = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // Use a queue for breadth-first traversal
        Queue<String> queue = new LinkedList<>();
        queue.add(entryPoint);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);
            Set<String> callees = getCallees(current);

            if (!callees.isEmpty()) {
                subGraph.put(current, new HashSet<>(callees));

                // Add unvisited callees to the queue
                for (String callee : callees) {
                    if (!visited.contains(callee)) {
                        queue.add(callee);
                    }
                }
            }
        }

        return subGraph;
    }
}

