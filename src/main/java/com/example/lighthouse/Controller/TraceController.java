package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.DatabaseConnection;
import com.example.lighthouse.Model.Trace;
import com.example.lighthouse.repository.DatabaseConnectionRepository;
import com.example.lighthouse.repository.TraceRepository;
import com.example.lighthouse.service.*;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired
    private SupabaseAuthService supabaseAuthService;

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
    public Trace executeQueryWithDB(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
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

        // Check if hallucination was detected and send email
        if (trace.getHallucinationData() != null && authentication != null) {
            try {
                HallucinationDetector.HallucinationResult result = gson.fromJson(
                        trace.getHallucinationData(),
                        HallucinationDetector.HallucinationResult.class
                );

                if (result != null && result.getConfidenceScore() < 50.0) {
                    String userEmail = supabaseAuthService.getUserEmail(authentication);
                    if (userEmail != null && emailService != null) {
                        System.out.println("📧 Sending email for query-with-db endpoint");
                        hallucinationDetector.notifyHallucination(
                                trace.getId(),
                                result,
                                userEmail,
                                trace.getPrompt(),
                                trace.getResponse()
                        );
                    } else {
                        System.out.println("⚠️ Email not sent - userEmail: " + userEmail + ", emailService: " + (emailService != null));
                    }
                }
            } catch (Exception e) {
                System.err.println("Error sending email notification in query-with-db: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return trace;
    }

    @PostMapping("/validate-response")
    public Trace validateResponse(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
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

        try {
            DatabaseConnection dbConfig = dbConnectionRepository.findById(dbConnectionId)
                    .orElseThrow(() -> new RuntimeException("Database connection not found"));

            String dbContext = externalDbService.searchDatabase(dbConfig, prompt);

            HallucinationDetector.HallucinationResult result =
                    hallucinationDetector.detectHallucinations(response, dbContext, prompt);

            trace.setHallucinationData(gson.toJson(result));
            trace.setConfidenceScore(result.getConfidenceScore());

            Trace savedTrace = traceRepository.save(trace);

            // Send email notification if hallucination detected
            if (result.getConfidenceScore() < 50.0 && authentication != null) {
                String userEmail = supabaseAuthService.getUserEmail(authentication);
                if (userEmail != null && emailService != null) {
                    System.out.println("📧 Sending email for validate-response endpoint");
                    hallucinationDetector.notifyHallucination(
                            savedTrace.getId(),
                            result,
                            userEmail,
                            prompt,
                            response
                    );
                } else {
                    System.out.println("⚠️ Email not sent - userEmail: " + userEmail + ", emailService: " + (emailService != null));
                }
            }

            return savedTrace;
        } catch (Exception e) {
            System.err.println("Hallucination detection error: " + e.getMessage());
            e.printStackTrace();
            // Still save the trace even if hallucination detection fails
            return traceRepository.save(trace);
        }
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
     * Check hallucinations for an existing trace using a database connection,
     * then notify via email if a hallucination is detected
     */
    @PostMapping("/{traceId}/check-hallucinations")
    public ResponseEntity<Trace> checkHallucinationsForTrace(
            @PathVariable String traceId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String dbConnectionId = request.get("dbConnectionId");
            if (dbConnectionId == null || dbConnectionId.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Trace trace = traceRepository.findById(traceId)
                    .orElseThrow(() -> new RuntimeException("Trace not found"));

            DatabaseConnection dbConnection = dbConnectionRepository.findById(dbConnectionId)
                    .orElseThrow(() -> new RuntimeException("Database connection not found"));

            // Get database context
            String databaseContext = externalDbService.searchDatabase(dbConnection, trace.getPrompt());

            // Run hallucination detection
            HallucinationDetector.HallucinationResult result =
                    hallucinationDetector.detectHallucinations(
                            trace.getResponse(),
                            databaseContext,
                            trace.getPrompt()
                    );

            // Store results
            trace.setHallucinationData(gson.toJson(result));
            trace.setConfidenceScore(result.getConfidenceScore());

            Trace updatedTrace = traceRepository.save(trace);

            // Send email notification if hallucination detected
            System.out.println("📧 Email check - Authentication: " + (authentication != null));
            System.out.println("📧 Email check - Email service: " + (emailService != null));
            System.out.println("📧 Email check - Confidence: " + result.getConfidenceScore() + "%");

            if (authentication != null && emailService != null) {
                String userEmail = supabaseAuthService.getUserEmail(authentication);
                System.out.println("📧 Email check - User email: " + userEmail);

                if (userEmail != null && result.getConfidenceScore() < 50.0) {
                    System.out.println("📧 Sending email notification for trace: " + traceId);
                    hallucinationDetector.notifyHallucination(
                            traceId,
                            result,
                            userEmail,
                            trace.getPrompt(),
                            trace.getResponse()
                    );
                } else {
                    if (userEmail == null) {
                        System.out.println("⚠️ Email not sent - userEmail is null");
                    } else {
                        System.out.println("⚠️ Email not sent - confidence " + result.getConfidenceScore() + "% >= 50%");
                    }
                }
            } else {
                System.out.println("⚠️ Email not sent - authentication: " + (authentication != null) + ", emailService: " + (emailService != null));
            }

            return ResponseEntity.ok(updatedTrace);
        } catch (Exception e) {
            System.err.println("Hallucination detection error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Test endpoint to verify email configuration
     */
    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.badRequest().body("No authentication");
        }

        String userEmail = supabaseAuthService.getUserEmail(authentication);
        if (userEmail == null) {
            return ResponseEntity.badRequest().body("No user email found. Principal: " + authentication.getName());
        }

        if (emailService == null) {
            return ResponseEntity.badRequest().body("Email service not configured");
        }

        try {
            emailService.sendHallucinationAlert(
                    userEmail,
                    "test-trace-123",
                    "Test prompt: What is the capital of France?",
                    "Test response: The capital of France is Paris.",
                    30.0,
                    2
            );
            return ResponseEntity.ok("Test email sent to: " + userEmail);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending test email: " + e.getMessage());
        }
    }
}