package com.grash.security;

import com.grash.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that applies rate limiting to authenticated users based on their user ID.
 * This filter should be added after authentication filters (JwtTokenFilter, ApiKeyAuthFilter)
 * to ensure the user is already authenticated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip rate limiting if disabled
        if (!rateLimiterService.isRateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get the current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Only apply rate limiting to authenticated users
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetail) {
            CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();
            Long userId = userDetail.getUser().getId();
            String userIdKey = "user:" + userId;

            // Check rate limit
            if (!rateLimiterService.resolveAuthenticatedUserBucket(userIdKey).tryConsume(1)) {
                log.warn("Rate limit exceeded for user ID: {}", userId);
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
