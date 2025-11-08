// src/main/java/com/example/lighthouse/Model/Trace.java
package com.example.lighthouse.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "traces")
public class Trace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String response;
    // Add to src/main/java/com/example/lighthouse/Model/Trace.java

    @Column(columnDefinition = "TEXT")
    private String hallucinationData; // JSON string with hallucination results

    private Double confidenceScore; // 0-100

    // Getters and Setters
    public String getHallucinationData() { return hallucinationData; }
    public void setHallucinationData(String hallucinationData) { this.hallucinationData = hallucinationData; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    private Integer tokensUsed;
    private Double costUsd;
    private Integer latencyMs;
    private String provider; // "openai", "anthropic"

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public Double getCostUsd() { return costUsd; }
    public void setCostUsd(Double costUsd) { this.costUsd = costUsd; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}