package com.example.lighthouse.filter;

import com.example.lighthouse.service.SupabaseAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private SupabaseAuthService supabaseAuthService;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.jwt.secret:}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Check if Supabase is configured
        boolean supabaseConfigured = supabaseUrl != null && !supabaseUrl.isEmpty()
                && jwtSecret != null && !jwtSecret.isEmpty();

        // If Supabase not configured, skip authentication and allow request through
        if (!supabaseConfigured || supabaseAuthService == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token provided - allow request through (for development)
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Verify token and get user info
            String userId = supabaseAuthService.verifyToken(token);

            if (userId != null) {
                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log error but don't block request (for development)
            logger.warn("JWT validation failed: " + e.getMessage());
            // Allow request through even if token is invalid (for development)
        }

        filterChain.doFilter(request, response);
    }
}