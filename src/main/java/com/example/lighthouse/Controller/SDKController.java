// src/main/java/com/example/lighthouse/controller/SDKController.java
package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.DatabaseConnection;
import com.example.lighthouse.Model.Project;
import com.example.lighthouse.Model.Trace;
import com.example.lighthouse.repository.DatabaseConnectionRepository;
import com.example.lighthouse.repository.ProjectRepository;
import com.example.lighthouse.repository.TraceRepository;
import com.example.lighthouse.service.ExternalDatabaseService;
import com.example.lighthouse.service.HallucinationDetector;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sdk")
@CrossOrigin(origins = "*")
public class SDKController {
    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatabaseConnectionRepository dbConnectionRepository;

    @Autowired
    private ExternalDatabaseService externalDbService;

    @Autowired
    private HallucinationDetector hallucinationDetector;

    private final Gson gson = new Gson();

    @PostMapping("/traces")
    public ResponseEntity<Map<String, Object>> receiveTrace(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody Map<String, Object> traceData
    ) {
        // Validate API key
        if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "API key is required. Include X-API-Key header."));
        }

        // Find project by API key
        Optional<Project> projectOpt = projectRepository.findByApiKey(apiKey);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid API key."));
        }

        Project project = projectOpt.get();

        try {
            // Create trace from SDK data
            Trace trace = new Trace();

            // Required fields
            trace.setPrompt(getStringValue(traceData, "prompt", ""));
            trace.setResponse(getStringValue(traceData, "response", ""));
            trace.setTokensUsed(getIntValue(traceData, "tokensUsed", 0));
            trace.setCostUsd(getDoubleValue(traceData, "costUsd", 0.0));
            trace.setLatencyMs(getIntValue(traceData, "latencyMs", 0));
            trace.setProvider(getStringValue(traceData, "provider", "unknown"));

            // Link to project
            trace.setProject(project);
            trace.setCreatedAt(java.time.LocalDateTime.now());

            // NEW: Check if databaseConnectionId is provided for hallucination detection
            String dbConnectionId = getStringValue(traceData, "databaseConnectionId", null);

            if (dbConnectionId != null && !dbConnectionId.isEmpty()) {
                try {
                    // Find database connection
                    Optional<DatabaseConnection> dbConfigOpt = dbConnectionRepository.findById(dbConnectionId);

                    if (dbConfigOpt.isPresent()) {
                        DatabaseConnection dbConfig = dbConfigOpt.get();

                        // Search database for context based on the prompt
                        String dbContext = externalDbService.searchDatabase(dbConfig, trace.getPrompt());

                        // Run hallucination detection
                        HallucinationDetector.HallucinationResult hallucinationResult =
                                hallucinationDetector.detectHallucinations(
                                        trace.getResponse(),
                                        dbContext,
                                        trace.getPrompt()
                                );

                        // Store hallucination results
                        String hallucinationJson = gson.toJson(hallucinationResult);
                        trace.setHallucinationData(hallucinationJson);
                        trace.setConfidenceScore(hallucinationResult.getConfidenceScore());

                        System.out.println("✅ Hallucination detection completed for SDK trace");
                        System.out.println("   Confidence Score: " + hallucinationResult.getConfidenceScore());
                    } else {
                        System.out.println("⚠️ Database connection not found: " + dbConnectionId + " - Skipping hallucination detection");
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error during hallucination detection: " + e.getMessage());
                    e.printStackTrace();
                    // Continue without hallucination detection - don't fail the trace
                }
            }

            // Optional: confidence score if provided directly
            if (traceData.containsKey("confidenceScore") && trace.getConfidenceScore() == null) {
                trace.setConfidenceScore(getDoubleValue(traceData, "confidenceScore", null));
            }

            // Save trace
            Trace savedTrace = traceRepository.save(trace);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "traceId", savedTrace.getId(),
                    "message", "Trace recorded successfully",
                    "confidenceScore", savedTrace.getConfidenceScore() != null ? savedTrace.getConfidenceScore() : "N/A"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "error", "Failed to process trace",
                            "message", e.getMessage()
                    ));
        }
    }

    // Helper methods
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}