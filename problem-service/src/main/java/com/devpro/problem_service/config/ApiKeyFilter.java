package com.devpro.problem_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${admin.service.key}")
    private String apiKey;

    private static final String HEADER_NAME = "X-Admin-service-key";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Allow all READ operations
        if ("GET".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/problems") && method.equalsIgnoreCase("POST")){
            String requestApiKey = request.getHeader(HEADER_NAME);
            if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or missing API Key");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
