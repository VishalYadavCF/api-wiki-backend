package com.redcat.tutorials.common;

public class StringUtils {
    public static String extractAndTransformFilePath(String fullPath) {
        // Extract everything after the last occurrence of "/target/classes/"
        String pattern = ".*/target/classes/(.+)\\.class$";
        String result = fullPath.replaceAll(pattern, "$1.java");
        return result;
    }

    // Alternative approach using indexOf
    public static String extractAndTransformFilePathAlternative(String fullPath) {
        String targetClasses = "/target/classes/";
        int index = fullPath.lastIndexOf(targetClasses);

        if (index != -1) {
            // Extract everything after "/target/classes/"
            String extracted = fullPath.substring(index + targetClasses.length());
            // Replace .class with .java
            return extracted.replace(".class", ".java");
        }

        return fullPath; // Return original if pattern not found
    }
}
