package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.DatabaseConnection;
import com.example.lighthouse.Model.Trace;
import com.example.lighthouse.repository.DatabaseConnectionRepository;
import com.example.lighthouse.repository.TraceRepository;
import com.example.lighthouse.service.AIService;
import com.example.lighthouse.service.ExternalDatabaseService;
import com.example.lighthouse.service.HallucinationDetector;
import com.google.gson.Gson;
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

    @Autowired
    private DatabaseConnectionRepository dbConnectionRepository;

    @Autowired
    private ExternalDatabaseService externalDbService;

    @Autowired
    private HallucinationDetector hallucinationDetector;

    private final Gson gson = new Gson();

    @GetMapping
    public List<Trace> getAllTraces(@RequestParam(required = false) String projectId) {
        if (projectId != null && !projectId.isEmpty()) {
            return traceRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        }
        return traceRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trace> getTrace(@PathVariable String id) {
        return traceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/query")
    public Trace executeQuery(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isEmpty()) {
            throw new RuntimeException("Prompt is required");
        }
        System.out.println("Executing query WITHOUT database: " + prompt);
        return aiService.executeQuery(prompt);
    }

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

    @PostMapping("/validate-response")
    public Trace validateResponse(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        String response = (String) request.get("response");
        String dbConnectionId = (String) request.get("databaseConnectionId");

        if (prompt == null || response == null || dbConnectionId == null) {
            throw new RuntimeException("prompt, response, and databaseConnectionId are required");
        }

        Trace trace = new Trace();
        trace.setPrompt(prompt);
        trace.setResponse(response);
        trace.setTokensUsed(((Number) request.getOrDefault("tokensUsed", 0)).intValue());
        trace.setCostUsd(((Number) request.getOrDefault("costUsd", 0.0)).doubleValue());
        trace.setLatencyMs(((Number) request.getOrDefault("latencyMs", 0)).intValue());
        trace.setProvider((String) request.getOrDefault("provider", "unknown"));

        // Run hallucination detection
        try {
            DatabaseConnection dbConfig = dbConnectionRepository.findById(dbConnectionId)
                    .orElseThrow(() -> new RuntimeException("Database connection not found"));

            String dbContext = externalDbService.searchDatabase(dbConfig, prompt);

            HallucinationDetector.HallucinationResult result =
                    hallucinationDetector.detectHallucinations(response, dbContext, prompt);

            trace.setHallucinationData(gson.toJson(result));
            trace.setConfidenceScore(result.getConfidenceScore());

        } catch (Exception e) {
            System.err.println("Hallucination detection error: " + e.getMessage());
            e.printStackTrace();
        }

        return traceRepository.save(trace);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(@RequestParam(required = false) String projectId) {
        Map<String, Object> stats = new HashMap<>();

        if (projectId != null && !projectId.isEmpty()) {
            Double totalCost = traceRepository.getTotalCostByProjectId(projectId);
            Long totalRequests = traceRepository.getTotalRequestsByProjectId(projectId);
            Double avgLatency = traceRepository.getAverageLatencyByProjectId(projectId);

            stats.put("totalCost", totalCost != null ? totalCost : 0.0);
            stats.put("totalRequests", totalRequests != null ? totalRequests : 0L);
            stats.put("averageLatency", avgLatency != null ? avgLatency : 0.0);
        } else {
            Double totalCost = traceRepository.getTotalCost();
            Long totalRequests = traceRepository.getTotalRequests();
            Double avgLatency = traceRepository.getAverageLatency();

            stats.put("totalCost", totalCost != null ? totalCost : 0.0);
            stats.put("totalRequests", totalRequests != null ? totalRequests : 0L);
            stats.put("averageLatency", avgLatency != null ? avgLatency : 0.0);
        }

        return stats;
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearTraces() {
        traceRepository.deleteAll();
        return ResponseEntity.ok().build();
    }


    /**
     * Check hallucinations for an existing trace using a database connection
     */
    @PostMapping("/{traceId}/check-hallucinations")
    public Trace checkHallucinationsForTrace(
            @PathVariable String traceId,
            @RequestBody Map<String, String> request
    ) {
        String dbConnectionId = request.get("dbConnectionId");

        if (dbConnectionId == null || dbConnectionId.isEmpty()) {
            throw new RuntimeException("dbConnectionId is required");
        }

        Trace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new RuntimeException("Trace not found"));

        // Skip if already has hallucination data
        if (trace.getHallucinationData() != null && !trace.getHallucinationData().isEmpty()) {
            return trace; // Already checked
        }

        try {
            DatabaseConnection dbConfig = dbConnectionRepository.findById(dbConnectionId)
                    .orElseThrow(() -> new RuntimeException("Database connection not found"));

            // Search database for context
            String dbContext = externalDbService.searchDatabase(dbConfig, trace.getPrompt());

            // Detect hallucinations
            HallucinationDetector.HallucinationResult result =
                    hallucinationDetector.detectHallucinations(
                            trace.getResponse(),
                            dbContext,
                            trace.getPrompt()
                    );

            // Update trace with results
            trace.setHallucinationData(gson.toJson(result));
            trace.setConfidenceScore(result.getConfidenceScore());

            return traceRepository.save(trace);

        } catch (Exception e) {
            System.err.println("Error checking hallucinations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to check hallucinations: " + e.getMessage());
        }
    }
}