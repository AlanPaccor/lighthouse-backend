package com.example.lighthouse.service;

import com.example.lighthouse.config.SupabaseConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
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
            return null;
        }
    }
}