// src/main/java/com/example/lighthouse/controller/SDKController.java
package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.Project;
import com.example.lighthouse.Model.Trace;
import com.example.lighthouse.repository.ProjectRepository;
import com.example.lighthouse.repository.TraceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sdk")
@CrossOrigin(origins = "*") // Allow external SDK calls from any origin
public class SDKController {
    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private ProjectRepository projectRepository;

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

            // Optional fields
            if (traceData.containsKey("confidenceScore")) {
                trace.setConfidenceScore(getDoubleValue(traceData, "confidenceScore", null));
            }

            // Link to project
            trace.setProject(project);
            trace.setCreatedAt(LocalDateTime.now());

            // Save trace
            Trace savedTrace = traceRepository.save(trace);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "traceId", savedTrace.getId(),
                    "message", "Trace recorded successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "error", "Failed to process trace",
                            "message", e.getMessage()
                    ));
        }
    }

    // Helper methods to safely extract values
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