package com.redcat.tutorials.gitcloner.impl;

import com.redcat.tutorials.gitcloner.CloneRepositoryException;
import com.redcat.tutorials.gitcloner.GitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
@Slf4j
public class BitbucketGitServiceImpl implements GitService {

    public BitbucketGitServiceImpl() {
    }

    @Override
    public void cloneRepository(String url) throws CloneRepositoryException {
        String targetDir = "cloned-repo/" + extractRepositoryName(url);

        ProcessBuilder builder = new ProcessBuilder("git", "clone", url, targetDir);
        builder.redirectErrorStream(true); // Merge error and output

        try {
            Process process = builder.start();

            // Read and print the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String extractRepositoryName(String gitUrl) {
        if (gitUrl == null || !gitUrl.contains(":") || !gitUrl.endsWith(".git")) {
            throw new IllegalArgumentException("Invalid Git URL: " + gitUrl);
        }

        String[] parts = gitUrl.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid Git URL format: " + gitUrl);
        }

        String path = parts[1]; // e.g., VishalYadavCF/Bookhub.git
        String repoName = path.substring(path.lastIndexOf("/") + 1, path.length() - 4); // Remove ".git"
        return repoName;
    }
}
