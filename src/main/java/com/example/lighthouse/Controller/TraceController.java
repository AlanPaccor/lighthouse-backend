// src/main/java/com/example/lighthouse/controller/TraceController.java
package com.example.lighthouse.controller;

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

    // Get all traces
    @GetMapping
    public List<Trace> getAllTraces() {
        return traceRepository.findTop100ByOrderByCreatedAtDesc();
    }

    // Get single trace by ID
    @GetMapping("/{id}")
    public ResponseEntity<Trace> getTrace(@PathVariable String id) {
        return traceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Execute AI query and create trace
    @PostMapping("/query")
    public Trace executeQuery(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return aiService.executeQuery(prompt);
    }

    // Get statistics
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCost", traceRepository.getTotalCost());
        stats.put("totalRequests", traceRepository.getTotalRequests());
        stats.put("averageLatency", traceRepository.getAverageLatency());
        return stats;
    }

    // Delete all traces (for demo reset)
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearTraces() {
        traceRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}