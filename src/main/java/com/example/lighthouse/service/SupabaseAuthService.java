package com.example.lighthouse.service;

import com.example.lighthouse.config.SupabaseConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class SupabaseAuthService {

    @Autowired
    private SupabaseConfig supabaseConfig;

    /**
     * Verify Supabase JWT token and extract user ID
     */
    public String verifyToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    supabaseConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8)
            );

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extract Supabase user ID (sub claim)
            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    supabaseConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8)
            );

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("email", String.class);
        } catch (Exception e) {
            System.err.println("Error extracting email from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get user email from Authentication object
     */
    public String getUserEmail(Authentication authentication) {
        if (authentication == null) {
            System.out.println("⚠️ getUserEmail: Authentication is null");
            return null;
        }

        try {
            // Try to get token from credentials
            Object credentials = authentication.getCredentials();
            if (credentials != null && credentials instanceof String) {
                String token = (String) credentials;
                String email = getEmailFromToken(token);
                if (email != null) {
                    System.out.println("✅ getUserEmail: Found email from token: " + email);
                    return email;
                }
            }

            // Try to parse email from principal name
            String principalName = authentication.getName();
            System.out.println("📧 getUserEmail: Principal name: " + principalName);

            if (principalName != null && principalName.contains("@")) {
                System.out.println("✅ getUserEmail: Using principal name as email: " + principalName);
                return principalName;
            }

            System.out.println("⚠️ getUserEmail: No email found");
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error getting user email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}