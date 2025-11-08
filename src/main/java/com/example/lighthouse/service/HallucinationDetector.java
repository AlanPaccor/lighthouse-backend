// src/main/java/com/example/lighthouse/service/HallucinationDetector.java
package com.example.lighthouse.service;

import com.example.lighthouse.Model.ApiCredential;
import com.example.lighthouse.repository.ApiCredentialRepository;
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
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class HallucinationDetector {

    @Value("${gemini.api.key}")
    private String defaultGeminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Autowired(required = false)
    private ApiCredentialRepository credentialRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public static class HallucinationResult {
        private double confidenceScore; // 0-100, higher = more confident
        private List<String> unsupportedClaims;
        private List<String> supportedClaims;
        private String aiReview;
        private boolean hasHallucinations;

        public HallucinationResult() {
            this.unsupportedClaims = new ArrayList<>();
            this.supportedClaims = new ArrayList<>();
            this.confidenceScore = 100.0;
            this.hasHallucinations = false;
        }

        // Getters and Setters
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

        public List<String> getUnsupportedClaims() { return unsupportedClaims; }
        public void setUnsupportedClaims(List<String> unsupportedClaims) { this.unsupportedClaims = unsupportedClaims; }

        public List<String> getSupportedClaims() { return supportedClaims; }
        public void setSupportedClaims(List<String> supportedClaims) { this.supportedClaims = supportedClaims; }

        public String getAiReview() { return aiReview; }
        public void setAiReview(String aiReview) { this.aiReview = aiReview; }

        public boolean isHasHallucinations() { return hasHallucinations; }
        public void setHasHallucinations(boolean hasHallucinations) { this.hasHallucinations = hasHallucinations; }
    }

    /**
     * Detect hallucinations by comparing AI response with source database context
     */
    public HallucinationResult detectHallucinations(String aiResponse, String databaseContext, String userPrompt) {
        HallucinationResult result = new HallucinationResult();

        // Step 1: Extract factual claims from AI response
        List<String> claims = extractClaims(aiResponse);

        if (claims.isEmpty()) {
            result.setConfidenceScore(100.0);
            result.setAiReview("No specific factual claims detected in the response.");
            return result;
        }

        // Step 2: Check each claim against database context
        for (String claim : claims) {
            if (isClaimSupported(claim, databaseContext)) {
                result.getSupportedClaims().add(claim);
            } else {
                result.getUnsupportedClaims().add(claim);
            }
        }

        // Step 3: Calculate confidence score
        if (claims.isEmpty()) {
            result.setConfidenceScore(100.0);
        } else {
            double supportedRatio = (double) result.getSupportedClaims().size() / claims.size();
            result.setConfidenceScore(supportedRatio * 100.0);
        }

        result.setHasHallucinations(!result.getUnsupportedClaims().isEmpty());

        // Step 4: Use AI to review and provide context (optional but recommended)
        if (result.isHasHallucinations() || result.getConfidenceScore() < 80.0) {
            result.setAiReview(generateAIReview(aiResponse, databaseContext, result));
        } else {
            result.setAiReview("All claims appear to be supported by the database.");
        }

        return result;
    }

    /**
     * Extract factual claims from AI response
     */
    private List<String> extractClaims(String response) {
        List<String> claims = new ArrayList<>();

        // Remove common phrases that aren't claims
        String cleaned = response.replaceAll("(?i)(according to|based on|the database shows|i found|i have)", "");

        // Extract sentences that contain factual information
        String[] sentences = cleaned.split("[.!?]+");

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() < 10) continue; // Skip very short sentences

            // Look for factual patterns
            if (containsFactualPattern(sentence)) {
                claims.add(sentence);
            }
        }

        // Also extract specific data points (numbers, dates, names)
        extractDataPoints(response, claims);

        return claims;
    }

    /**
     * Check if sentence contains factual patterns
     */
    private boolean containsFactualPattern(String sentence) {
        // Patterns that indicate factual claims
        String[] patterns = {
                "\\d+",  // Contains numbers
                "\\b(is|was|are|were|has|have|had)\\b",  // State of being
                "\\b(\\d{4})\\b",  // Years
                "\\b([A-Z][a-z]+ [A-Z][a-z]+)\\b",  // Proper names
        };

        for (String pattern : patterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(sentence).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract specific data points (numbers, dates, names)
     */
    private void extractDataPoints(String response, List<String> claims) {
        // Extract years
        Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");
        Matcher yearMatcher = yearPattern.matcher(response);
        while (yearMatcher.find()) {
            claims.add("Year: " + yearMatcher.group());
        }

        // Extract numbers that might be counts or metrics
        Pattern numberPattern = Pattern.compile("\\b\\d+\\b");
        Matcher numberMatcher = numberPattern.matcher(response);
        Set<String> numbers = new HashSet<>();
        while (numberMatcher.find()) {
            String num = numberMatcher.group();
            if (num.length() <= 4) { // Reasonable numbers
                numbers.add("Number: " + num);
            }
        }
        claims.addAll(numbers);
    }

    /**
     * Check if a claim is supported by database context
     */
    private boolean isClaimSupported(String claim, String databaseContext) {
        if (databaseContext == null || databaseContext.trim().isEmpty()) {
            return false;
        }

        // Normalize for comparison
        String normalizedClaim = claim.toLowerCase().trim();
        String normalizedContext = databaseContext.toLowerCase();

        // Extract key terms from claim
        String[] claimWords = normalizedClaim.split("\\s+");
        List<String> significantWords = new ArrayList<>();

        for (String word : claimWords) {
            // Skip common words
            if (!isStopWord(word) && word.length() > 3) {
                significantWords.add(word);
            }
        }

        if (significantWords.isEmpty()) {
            return true; // No significant words to check
        }

        // Check if at least 50% of significant words appear in context
        int matches = 0;
        for (String word : significantWords) {
            if (normalizedContext.contains(word)) {
                matches++;
            }
        }

        double matchRatio = (double) matches / significantWords.size();
        return matchRatio >= 0.5; // At least 50% match
    }

    /**
     * Check if word is a stop word (common word that doesn't add meaning)
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "from", "as", "is", "was", "are", "were", "be",
                "been", "being", "have", "has", "had", "do", "does", "did", "will",
                "would", "could", "should", "may", "might", "must", "can", "this",
                "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
        );
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * Use AI to review hallucinations and provide context
     */
    private String generateAIReview(String aiResponse, String databaseContext, HallucinationResult result) {
        try {
            String apiKey = getApiKey();

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return "Unable to generate AI review: No API key available.";
            }

            String reviewPrompt = String.format(
                    "You are a fact-checker reviewing an AI response against source database data.\n\n" +
                            "DATABASE DATA:\n%s\n\n" +
                            "AI RESPONSE:\n%s\n\n" +
                            "UNSUPPORTED CLAIMS DETECTED:\n%s\n\n" +
                            "SUPPORTED CLAIMS:\n%s\n\n" +
                            "Please provide a brief review (2-3 sentences):\n" +
                            "1. Identify which claims are not supported by the database\n" +
                            "2. Explain what information is missing\n" +
                            "3. Rate the overall reliability (High/Medium/Low)",
                    databaseContext.length() > 2000 ? databaseContext.substring(0, 2000) + "..." : databaseContext,
                    aiResponse.length() > 1000 ? aiResponse.substring(0, 1000) + "..." : aiResponse,
                    result.getUnsupportedClaims().isEmpty() ? "None" : String.join("\n- ", result.getUnsupportedClaims()),
                    result.getSupportedClaims().isEmpty() ? "None" : String.join("\n- ", result.getSupportedClaims())
            );

            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", reviewPrompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiApiUrl))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                return extractReviewFromResponse(responseJson);
            } else {
                return "Unable to generate AI review (API error: " + response.statusCode() + ")";
            }
        } catch (Exception e) {
            return "Unable to generate AI review: " + e.getMessage();
        }
    }

    private String extractReviewFromResponse(JsonObject responseJson) {
        try {
            if (responseJson.has("candidates")) {
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts != null && parts.size() > 0) {
                                JsonObject firstPart = parts.get(0).getAsJsonObject();
                                if (firstPart.has("text")) {
                                    return firstPart.get("text").getAsString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting review: " + e.getMessage());
            e.printStackTrace();
        }
        return "Review generation failed";
    }

    /**
     * Get API key - tries user's key first, then falls back to default
     */
    private String getApiKey() {
        // Try to get user's API key from repository
        if (credentialRepository != null) {
            try {
                Optional<ApiCredential> cred = credentialRepository.findByProviderAndIsActiveTrue("gemini");
                if (cred.isPresent() && cred.get().getApiKey() != null && !cred.get().getApiKey().trim().isEmpty()) {
                    return cred.get().getApiKey();
                }
            } catch (Exception e) {
                System.err.println("Error getting user API key: " + e.getMessage());
            }
        }

        // Fallback to default key from config
        return defaultGeminiApiKey;
    }
}