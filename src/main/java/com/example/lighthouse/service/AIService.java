// src/main/java/com/example/lighthouse/service/AIService.java
package com.example.lighthouse.service;

import com.example.lighthouse.Model.Trace;
import com.example.lighthouse.repository.TraceRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AIService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Autowired
    private TraceRepository traceRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public Trace executeQuery(String prompt) {
        // Create trace
        Trace trace = new Trace();
        trace.setPrompt(prompt);
        trace.setProvider("gemini");

        // Measure latency
        long startTime = System.currentTimeMillis();

        try {
            // Build Gemini request body
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            // Make HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiApiUrl))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", geminiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response
            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            String aiResponse = extractResponse(responseJson);

            // Calculate metrics
            long endTime = System.currentTimeMillis();
            int latency = (int)(endTime - startTime);
            int tokensUsed = estimateTokens(prompt, aiResponse);
            double cost = calculateCost(tokensUsed, "gemini-2.0-flash");

            // Set trace data
            trace.setResponse(aiResponse);
            trace.setLatencyMs(latency);
            trace.setTokensUsed(tokensUsed);
            trace.setCostUsd(cost);

        } catch (Exception e) {
            trace.setResponse("Error: " + e.getMessage());
            trace.setLatencyMs((int)(System.currentTimeMillis() - startTime));
            trace.setTokensUsed(0);
            trace.setCostUsd(0.0);
            e.printStackTrace();
        }

        // Save and return
        return traceRepository.save(trace);
    }

    private String extractResponse(JsonObject responseJson) {
        try {
            return responseJson
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return "Unable to parse response";
        }
    }

    private int estimateTokens(String prompt, String response) {
        // Rough estimate: 1 token ≈ 4 characters
        int totalChars = prompt.length() + response.length();
        return totalChars / 4;
    }

    private double calculateCost(int tokens, String model) {
        // Gemini 2.0 Flash pricing (as of late 2024):
        // Input: $0.075 per 1M tokens
        // Output: $0.30 per 1M tokens
        // Simplified average: $0.1875 per 1M tokens
        double costPer1M = 0.1875;
        return (tokens / 1_000_000.0) * costPer1M;
    }
}