// src/main/java/com/example/lighthouse/repository/DocumentRepository.java
package com.example.lighthouse.repository;

import com.example.lighthouse.Model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {

    // Simple keyword search (case-insensitive)
    @Query("SELECT d FROM Document d WHERE LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Document> searchByKeyword(@Param("keyword") String keyword);

    // Find by category
    List<Document> findByCategory(String category);

    // Get all documents ordered by date
    List<Document> findAllByOrderByCreatedAtDesc();
}