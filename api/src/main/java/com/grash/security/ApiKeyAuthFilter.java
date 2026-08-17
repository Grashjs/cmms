package com.grash.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.model.ApiKey;
import com.grash.model.User;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.ApiKeyRepository;
import com.grash.service.LicenseService;
import com.grash.utils.Helper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final Map<Long, Instant> lastUsedCache = new ConcurrentHashMap<>();
    private static final Duration UPDATE_INTERVAL = Duration.ofMinutes(5);
    private final LicenseService licenseService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @org.jetbrains.annotations.NotNull HttpServletResponse response,
                                    @org.jetbrains.annotations.NotNull FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader("x-api-key");

        if (apiKey != null) {
            try {
                Optional<ApiKey> foundKey = apiKeyRepository.findByCode(Helper.hashKey(apiKey));
                if (foundKey.isPresent()) {
                    ApiKey key = foundKey.get();
                    if (key.getRevokedAt() != null) {
                        sendError(response, HttpStatus.UNAUTHORIZED, "API key has been revoked");
                        return;
                    }
                    if (key.getExpiresAt() != null && key.getExpiresAt().before(new Date())) {
                        sendError(response, HttpStatus.UNAUTHORIZED, "API key has expired");
                        return;
                    }
                    User user = key.getUser();
                    CustomUserDetail customUserDetail =
                            CustomUserDetail.builder().user(user).build();
                    if (!customUserDetail.isEnabled()) {
                        sendError(response, HttpStatus.UNAUTHORIZED, "User account is disabled");
                        return;
                    }
                    if (!(licenseService.hasEntitlement(LicenseEntitlement.API_ACCESS) && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.API_ACCESS))) {
                        sendError(response, HttpStatus.FORBIDDEN, "Access denied");
                        return;
                    }
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            customUserDetail,
                            null,
                            customUserDetail.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    updateLastUsedIfNeeded(key);
                }
            } catch (NoSuchAlgorithmException e) {
                sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) {
        try {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    Map.of("success", false, "message", message));
        } catch (IOException e) {
            logger.error("Failed to write error response", e);
        }
    }

    private void updateLastUsedIfNeeded(ApiKey key) {
        Instant now = Instant.now();
        Instant lastPersisted = lastUsedCache.get(key.getId());

        if (lastPersisted == null || now.isAfter(lastPersisted.plus(UPDATE_INTERVAL))) {
            lastUsedCache.put(key.getId(), now);
            apiKeyRepository.updateLastUsed(key.getId(), Date.from(now));
        }
    }
}
