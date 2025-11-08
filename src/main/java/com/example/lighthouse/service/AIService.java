// src/main/java/com/example/lighthouse/service/AIService.java
package com.example.lighthouse.service;

import com.example.lighthouse.Model.Document;
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
import java.util.List;

@Service
public class AIService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private RAGService ragService;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public Trace executeQueryWithRAG(String userPrompt) {
        // Create trace
        Trace trace = new Trace();
        trace.setPrompt(userPrompt);
        trace.setProvider("gemini");

        // Measure latency
        long startTime = System.currentTimeMillis();

        try {
            // 1. Find relevant documents
            List<Document> relevantDocs = ragService.findRelevantDocuments(userPrompt, 3);

            // 2. Build context from documents
            String context = ragService.buildContext(relevantDocs);

            // 3. Create enhanced prompt with context
            String enhancedPrompt = String.format(
                    "%s\n\nUser Question: %s\n\nPlease answer based on the information provided above. If the information doesn't contain the answer, say so.",
                    context,
                    userPrompt
            );

            // 4. Build Gemini request body
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", enhancedPrompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            // 5. Make HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiApiUrl))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", geminiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 6. Parse response
            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            String aiResponse = extractResponse(responseJson);

            // 7. Calculate metrics
            long endTime = System.currentTimeMillis();
            int latency = (int)(endTime - startTime);
            int tokensUsed = estimateTokens(enhancedPrompt, aiResponse);
            double cost = calculateCost(tokensUsed, "gemini-2.0-flash");

            // 8. Set trace data
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

    // Keep the old method for non-RAG queries
    public Trace executeQuery(String prompt) {
        return executeQueryWithRAG(prompt); // Now uses RAG by default
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
        int totalChars = prompt.length() + response.length();
        return totalChars / 4;
    }

    private double calculateCost(int tokens, String model) {
        double costPer1M = 0.1875;
        return (tokens / 1_000_000.0) * costPer1M;
    }
}