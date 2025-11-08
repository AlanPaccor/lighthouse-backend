// src/main/java/com/example/lighthouse/service/AIService.java
package com.example.lighthouse.service;

import com.example.lighthouse.Model.DatabaseConnection;
import com.example.lighthouse.Model.Trace;
import com.example.lighthouse.repository.DatabaseConnectionRepository;
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

    @Autowired
    private ExternalDatabaseService externalDbService;

    @Autowired
    private DatabaseConnectionRepository dbConnectionRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // Execute query without database
    public Trace executeQuery(String prompt) {
        Trace trace = new Trace();
        trace.setPrompt(prompt);
        trace.setProvider("gemini");

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

            // Log the raw response for debugging
            System.out.println("=== GEMINI RAW RESPONSE (NO DB) ===");
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
            System.out.println("===================================");

            // Handle different status codes
            if (response.statusCode() == 429) {
                String errorMsg = "Rate limit exceeded. Please wait a moment and try again. Gemini API has rate limits on free tier.";
                trace.setResponse(errorMsg);
                trace.setLatencyMs((int)(System.currentTimeMillis() - startTime));
                trace.setTokensUsed(0);
                trace.setCostUsd(0.0);
                return traceRepository.save(trace);
            }

            if (response.statusCode() != 200) {
                // Try to parse error message
                try {
                    JsonObject errorJson = gson.fromJson(response.body(), JsonObject.class);
                    if (errorJson.has("error")) {
                        JsonObject error = errorJson.getAsJsonObject("error");
                        String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                        trace.setResponse("Gemini API Error: " + message);
                    } else {
                        trace.setResponse("Gemini API returned status: " + response.statusCode());
                    }
                } catch (Exception e) {
                    trace.setResponse("Gemini API returned status: " + response.statusCode() + ", Body: " + response.body());
                }
                trace.setLatencyMs((int)(System.currentTimeMillis() - startTime));
                trace.setTokensUsed(0);
                trace.setCostUsd(0.0);
                return traceRepository.save(trace);
            }

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

        return traceRepository.save(trace);
    }

    // Execute query with external database
    public Trace executeQueryWithExternalDB(String userPrompt, String dbConnectionId) {
        Trace trace = new Trace();
        trace.setPrompt(userPrompt);
        trace.setProvider("gemini");

        long startTime = System.currentTimeMillis();

        try {
            // 1. Get database connection
            DatabaseConnection dbConfig = dbConnectionRepository.findById(dbConnectionId)
                    .orElseThrow(() -> new RuntimeException("Database connection not found"));

            // 2. Search database for relevant data
            String dbContext = externalDbService.searchDatabase(dbConfig, userPrompt);

            // Log the context for debugging
            System.out.println("=== DATABASE CONTEXT ===");
            System.out.println(dbContext);
            System.out.println("========================");

            // 3. Create enhanced prompt with DB data
            String enhancedPrompt = String.format(
                    "You are a helpful assistant with access to a database. Below is data retrieved from the database based on the user's question.\n\n" +
                            "DATABASE DATA:\n%s\n\n" +
                            "USER QUESTION: %s\n\n" +
                            "INSTRUCTIONS:\n" +
                            "- Answer the user's question using ONLY the database data provided above.\n" +
                            "- If the database data contains the answer, provide it clearly and accurately.\n" +
                            "- If the database data does NOT contain the answer, say: \"I do not have information about [topic] in the database.\"\n" +
                            "- Do not make up information that is not in the database data.\n" +
                            "- Format your response in a clear, readable way.",
                    dbContext,
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

            // Log the raw response for debugging
            System.out.println("=== GEMINI RAW RESPONSE (WITH DB) ===");
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
            System.out.println("=====================================");

            // Handle different status codes
            if (response.statusCode() == 429) {
                String errorMsg = "⚠️ Rate limit exceeded. Please wait a moment and try again.\n\n" +
                        "Gemini API has rate limits. You may have made too many requests too quickly.\n" +
                        "Please wait 30-60 seconds before trying again.";
                trace.setResponse(errorMsg);
                trace.setLatencyMs((int)(System.currentTimeMillis() - startTime));
                trace.setTokensUsed(0);
                trace.setCostUsd(0.0);
                return traceRepository.save(trace);
            }

            if (response.statusCode() != 200) {
                // Try to parse error message
                try {
                    JsonObject errorJson = gson.fromJson(response.body(), JsonObject.class);
                    if (errorJson.has("error")) {
                        JsonObject error = errorJson.getAsJsonObject("error");
                        String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                        trace.setResponse("Gemini API Error: " + message);
                    } else {
                        trace.setResponse("Gemini API returned status: " + response.statusCode());
                    }
                } catch (Exception e) {
                    trace.setResponse("Gemini API returned status: " + response.statusCode() + ", Body: " + response.body());
                }
                trace.setLatencyMs((int)(System.currentTimeMillis() - startTime));
                trace.setTokensUsed(0);
                trace.setCostUsd(0.0);
                return traceRepository.save(trace);
            }

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

        return traceRepository.save(trace);
    }

    private String extractResponse(JsonObject responseJson) {
        try {
            // Check if there's an error first
            if (responseJson.has("error")) {
                JsonObject error = responseJson.getAsJsonObject("error");
                String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                return "Error from Gemini API: " + message;
            }

            // Check if candidates array exists
            if (!responseJson.has("candidates") || responseJson.get("candidates").isJsonNull()) {
                // Log the full response for debugging
                System.err.println("=== GEMINI RESPONSE (no candidates) ===");
                System.err.println(responseJson.toString());
                System.err.println("========================================");
                return "Error: No candidates in response. Check backend logs for full response.";
            }

            JsonArray candidates = responseJson.getAsJsonArray("candidates");

            if (candidates == null || candidates.size() == 0) {
                System.err.println("=== GEMINI RESPONSE (empty candidates) ===");
                System.err.println(responseJson.toString());
                System.err.println("==========================================");
                return "Error: Empty candidates array. Check backend logs for full response.";
            }

            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();

            // Check if content exists
            if (!firstCandidate.has("content") || firstCandidate.get("content").isJsonNull()) {
                System.err.println("=== GEMINI RESPONSE (no content) ===");
                System.err.println(responseJson.toString());
                System.err.println("====================================");
                return "Error: No content in candidate. Check backend logs for full response.";
            }

            JsonObject content = firstCandidate.getAsJsonObject("content");

            // Check if parts exists
            if (!content.has("parts") || content.get("parts").isJsonNull()) {
                System.err.println("=== GEMINI RESPONSE (no parts) ===");
                System.err.println(responseJson.toString());
                System.err.println("==================================");
                return "Error: No parts in content. Check backend logs for full response.";
            }

            JsonArray parts = content.getAsJsonArray("parts");

            if (parts == null || parts.size() == 0) {
                System.err.println("=== GEMINI RESPONSE (empty parts) ===");
                System.err.println(responseJson.toString());
                System.err.println("=====================================");
                return "Error: Empty parts array. Check backend logs for full response.";
            }

            JsonObject firstPart = parts.get(0).getAsJsonObject();

            // Check if text exists
            if (!firstPart.has("text") || firstPart.get("text").isJsonNull()) {
                System.err.println("=== GEMINI RESPONSE (no text) ===");
                System.err.println(responseJson.toString());
                System.err.println("=================================");
                return "Error: No text in part. Check backend logs for full response.";
            }

            return firstPart.get("text").getAsString();

        } catch (Exception e) {
            // Log the full response when parsing fails
            System.err.println("=== EXCEPTION PARSING GEMINI RESPONSE ===");
            System.err.println("Exception: " + e.getMessage());
            System.err.println("Response JSON: " + responseJson.toString());
            System.err.println("==========================================");
            e.printStackTrace();
            return "Unable to parse response: " + e.getMessage() + ". Check backend logs for full response.";
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