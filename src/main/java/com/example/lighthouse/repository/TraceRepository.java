// src/main/java/com/example/lighthouse/repository/TraceRepository.java
package com.example.lighthouse.repository;

import com.example.lighthouse.Model.Trace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TraceRepository extends JpaRepository<Trace, String> {
    List<Trace> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT SUM(t.costUsd) FROM Trace t")
    Double getTotalCost();

    @Query("SELECT AVG(t.latencyMs) FROM Trace t")
    Double getAverageLatency();

    @Query("SELECT COUNT(t) FROM Trace t")
    Long getTotalRequests();

// In your existing TraceRepository.java - ADD THIS METHOD
// Keep all existing methods, just add:

    List<Trace> findByProjectIdOrderByCreatedAtDesc(String projectId);

    // If you want stats by project:
    @Query("SELECT SUM(t.costUsd) FROM Trace t WHERE t.project.id = :projectId")
    Double getTotalCostByProjectId(@Param("projectId") String projectId);

    @Query("SELECT AVG(t.latencyMs) FROM Trace t WHERE t.project.id = :projectId")
    Double getAverageLatencyByProjectId(@Param("projectId") String projectId);

    @Query("SELECT COUNT(t) FROM Trace t WHERE t.project.id = :projectId")
    Long getTotalRequestsByProjectId(@Param("projectId") String projectId);
}
