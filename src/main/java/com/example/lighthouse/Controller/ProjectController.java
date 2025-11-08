// src/main/java/com/example/lighthouse/controller/ProjectController.java
package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.Project;
import com.example.lighthouse.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectController {
    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable String id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Project createProject(@RequestBody Map<String, String> request) {
        Project project = new Project();
        project.setName(request.get("name"));
        project.setDescription(request.getOrDefault("description", ""));

        // Generate API key: lh_ + random UUID (without dashes)
        String apiKey = "lh_" + UUID.randomUUID().toString().replace("-", "");
        project.setApiKey(apiKey);

        project.setCreatedAt(LocalDateTime.now());

        return projectRepository.save(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}