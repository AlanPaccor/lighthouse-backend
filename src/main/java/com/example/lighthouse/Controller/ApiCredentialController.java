// src/main/java/com/example/lighthouse/controller/ApiCredentialController.java
package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.ApiCredential;
import com.example.lighthouse.repository.ApiCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/credentials")
@CrossOrigin(origins = "http://localhost:5173")
public class ApiCredentialController {

    @Autowired
    private ApiCredentialRepository credentialRepository;

    // Get API key for a provider (returns masked key)
    @GetMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> getCredential(@PathVariable String provider) {
        Optional<ApiCredential> cred = credentialRepository.findByProvider(provider);

        Map<String, Object> response = new HashMap<>();
        if (cred.isPresent()) {
            ApiCredential c = cred.get();
            response.put("exists", true);
            response.put("isActive", c.getIsActive());
            // Mask the API key (show only last 4 characters)
            String maskedKey = maskApiKey(c.getApiKey());
            response.put("apiKeyMasked", maskedKey);
        } else {
            response.put("exists", false);
        }

        return ResponseEntity.ok(response);
    }

    // Save or update API key
    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> saveCredential(
            @PathVariable String provider,
            @RequestBody Map<String, String> request
    ) {
        String apiKey = request.get("apiKey");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "API key is required");
            return ResponseEntity.badRequest().body(error);
        }

        // Test the API key by making a simple request
        boolean isValid = testApiKey(provider, apiKey);

        Optional<ApiCredential> existing = credentialRepository.findByProvider(provider);
        ApiCredential credential;

        if (existing.isPresent()) {
            credential = existing.get();
            credential.setApiKey(apiKey);
            credential.setIsActive(isValid);
            credential.setUpdatedAt(LocalDateTime.now());
        } else {
            credential = new ApiCredential();
            credential.setProvider(provider);
            credential.setApiKey(apiKey);
            credential.setIsActive(isValid);
        }

        credentialRepository.save(credential);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", isValid ? "API key saved and verified!" : "API key saved but verification failed");
        response.put("isValid", isValid);

        return ResponseEntity.ok(response);
    }

    // Delete API key
    @DeleteMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> deleteCredential(@PathVariable String provider) {
        Optional<ApiCredential> cred = credentialRepository.findByProvider(provider);
        if (cred.isPresent()) {
            credentialRepository.delete(cred.get());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "API key deleted");
        return ResponseEntity.ok(response);
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }

    private boolean testApiKey(String provider, String apiKey) {
        if (!provider.equals("gemini")) {
            return false;
        }

        try {
            // Make a simple test request to verify the key works
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", apiKey)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                            "{\"contents\":[{\"parts\":[{\"text\":\"test\"}]}]}"
                    ))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("API key test failed: " + e.getMessage());
            return false;
        }
    }
}
