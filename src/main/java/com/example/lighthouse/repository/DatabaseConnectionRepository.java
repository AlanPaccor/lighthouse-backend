// src/main/java/com/example/lighthouse/repository/DatabaseConnectionRepository.java
package com.example.lighthouse.repository;

import com.example.lighthouse.Model.DatabaseConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, String> {
    List<DatabaseConnection> findAllByOrderByCreatedAtDesc();
}