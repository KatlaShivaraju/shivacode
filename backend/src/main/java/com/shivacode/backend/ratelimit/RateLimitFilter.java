
package com.shivacode.backend.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Only rate-limit POST requests.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        String ipAddress = request.getRemoteAddr();

        boolean allowed = true;

        switch (path) {

            case "/api/auth/register" ->
                    allowed = rateLimitService.allowRegistration(ipAddress);

            case "/api/auth/login" ->
                    allowed = rateLimitService.allowLogin(ipAddress);

            case "/api/execute" ->
                    allowed = rateLimitService.allowCodeExecution(ipAddress);

            case "/api/execute/batch" ->
                    allowed = rateLimitService.allowBatchExecution(ipAddress);

            default -> {
                if (path.matches("/api/problems/\\d+/submit")) {
                    allowed = rateLimitService.allowSubmission(ipAddress);
                }
            }
        }

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please try again later.\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}


