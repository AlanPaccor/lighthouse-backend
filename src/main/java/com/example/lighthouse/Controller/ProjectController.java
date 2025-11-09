package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.Project;
import com.example.lighthouse.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    // Get all projects for the authenticated user (or all if no auth)
    @GetMapping
    public List<Project> getAllProjects(Authentication authentication) {
        // If authentication is present, filter by user ID
        if (authentication != null && authentication.getName() != null) {
            String userId = authentication.getName();
            return projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        // If no authentication, return all projects (for development)
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get a specific project by ID
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable String id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create a new project linked to the authenticated user (or anonymous)
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Map<String, String> request, Authentication authentication) {
        Project project = new Project();
        project.setName(request.get("name"));
        project.setDescription(request.getOrDefault("description", ""));

        // Set user ID if authenticated, otherwise use "anonymous" for development
        if (authentication != null && authentication.getName() != null) {
            project.setUserId(authentication.getName());
        } else {
            // For development: use "anonymous" when Supabase isn't configured
            project.setUserId("anonymous");
        }

        // Generate API key: lh_ + random UUID (without dashes)
        String apiKey = "lh_" + UUID.randomUUID().toString().replace("-", "");
        project.setApiKey(apiKey);
        project.setCreatedAt(LocalDateTime.now());

        Project savedProject = projectRepository.save(project);
        return ResponseEntity.ok(savedProject);
    }

    // Delete a project by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}