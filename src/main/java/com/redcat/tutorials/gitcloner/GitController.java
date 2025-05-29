package com.redcat.tutorials.gitcloner;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/git/v1")
public class GitController {

    private final GitService gitService;

    public GitController(GitService gitService) {
        this.gitService = gitService;
    }

    // create an endpoint to clone a bit bucket url using gitService interface method
     @PostMapping("/clone")
     public ResponseEntity<String> cloneRepository(@RequestBody Map<String, Object> req) {
         try {
             gitService.cloneRepository((String)req.get("url"));
             return ResponseEntity.ok("Repository cloned successfully");
         } catch (CloneRepositoryException e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error cloning repository: " + e.getMessage());
         }
     }

     // clone multiple repositories simultaneously
     @PostMapping("/clone-many")
     public ResponseEntity<Map<String, String>> cloneMultipleRepositories(@RequestBody Map<String, Object> req) {
         Map<String, String> results = new HashMap<>();

         @SuppressWarnings("unchecked")
         List<String> urls = (List<String>) req.get("urls");

         if (urls == null || urls.isEmpty()) {
             return ResponseEntity.badRequest().body(Collections.singletonMap("error", "No repository URLs provided"));
         }

         for (String url : urls) {
             try {
                 gitService.cloneRepository(url);
                 results.put(url, "Repository cloned successfully");
             } catch (CloneRepositoryException e) {
                 results.put(url, "Error cloning repository: " + e.getMessage());
             }
         }

         return ResponseEntity.ok(results);
     }
}
