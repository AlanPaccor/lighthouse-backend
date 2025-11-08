// src/main/java/com/example/lighthouse/controller/TraceController.java
package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.Trace;
import com.example.lighthouse.repository.TraceRepository;
import com.example.lighthouse.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traces")
@CrossOrigin(origins = "http://localhost:5173")
public class TraceController {

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private AIService aiService;

    // Get all traces (with optional project filter)
    @GetMapping
    public List<Trace> getAllTraces(@RequestParam(required = false) String projectId) {
        // If projectId provided, filter by project
        if (projectId != null && !projectId.isEmpty()) {
            return traceRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        }
        // Default: return all traces (backward compatible)
        return traceRepository.findTop100ByOrderByCreatedAtDesc();
    }

    // Get single trace by ID
    @GetMapping("/{id}")
    public ResponseEntity<Trace> getTrace(@PathVariable String id) {
        return traceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Execute AI query WITHOUT database (regular query)
    @PostMapping("/query")
    public Trace executeQuery(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isEmpty()) {
            throw new RuntimeException("Prompt is required");
        }
        System.out.println("Executing query WITHOUT database: " + prompt);
        return aiService.executeQuery(prompt);
    }

    // Execute query WITH external database connection
    @PostMapping("/query-with-db")
    public Trace executeQueryWithDB(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String dbConnectionId = request.get("dbConnectionId");

        if (prompt == null || prompt.isEmpty()) {
            throw new RuntimeException("Prompt is required");
        }
        if (dbConnectionId == null || dbConnectionId.isEmpty()) {
            throw new RuntimeException("Database connection ID is required");
        }

        System.out.println("Executing query WITH database connection: " + dbConnectionId);
        System.out.println("Query: " + prompt);

        Trace trace = aiService.executeQueryWithExternalDB(prompt, dbConnectionId);
        return trace;
    }

    // Get statistics
    @GetMapping("/stats")
    public Map<String, Object> getStats(@RequestParam(required = false) String projectId) {
        Map<String, Object> stats = new HashMap<>();

        if (projectId != null && !projectId.isEmpty()) {
            // Filter by project
            Double totalCost = traceRepository.getTotalCostByProjectId(projectId);
            Long totalRequests = traceRepository.getTotalRequestsByProjectId(projectId);
            Double avgLatency = traceRepository.getAverageLatencyByProjectId(projectId);

            stats.put("totalCost", totalCost != null ? totalCost : 0.0);
            stats.put("totalRequests", totalRequests != null ? totalRequests : 0L);
            stats.put("averageLatency", avgLatency != null ? avgLatency : 0.0);
        } else {
            // Default: all stats (backward compatible)
            Double totalCost = traceRepository.getTotalCost();
            Long totalRequests = traceRepository.getTotalRequests();
            Double avgLatency = traceRepository.getAverageLatency();

            stats.put("totalCost", totalCost != null ? totalCost : 0.0);
            stats.put("totalRequests", totalRequests != null ? totalRequests : 0L);
            stats.put("averageLatency", avgLatency != null ? avgLatency : 0.0);
        }

        return stats;
    }

    // Delete all traces (for demo reset)
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearTraces() {
        traceRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}