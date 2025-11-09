// src/main/java/com/example/lighthouse/repository/ProjectRepository.java
package com.example.lighthouse.repository;

import com.example.lighthouse.Model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, String> {
    Optional<Project> findByApiKey(String apiKey);
    List<Project> findAllByOrderByCreatedAtDesc();
    List<Project> findByUserIdOrderByCreatedAtDesc(String userId);
}