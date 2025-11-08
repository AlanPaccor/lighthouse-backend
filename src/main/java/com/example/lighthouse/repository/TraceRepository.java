// src/main/java/com/example/lighthouse/repository/TraceRepository.java
package com.example.lighthouse.repository;

import com.example.lighthouse.Model.Trace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TraceRepository extends JpaRepository<Trace, String> {
    List<Trace> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT SUM(t.costUsd) FROM Trace t")
    Double getTotalCost();

    @Query("SELECT AVG(t.latencyMs) FROM Trace t")
    Double getAverageLatency();

    @Query("SELECT COUNT(t) FROM Trace t")
    Long getTotalRequests();
}