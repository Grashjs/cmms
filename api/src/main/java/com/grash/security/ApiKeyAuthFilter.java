package com.grash.security;

import com.grash.model.ApiKey;
import com.grash.model.OwnUser;
import com.grash.repository.ApiKeyRepository;
import com.grash.security.CustomUserDetail;
import com.grash.utils.Helper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    private final Map<Long, Instant> lastUsedCache = new ConcurrentHashMap<>();
    private static final Duration UPDATE_INTERVAL = Duration.ofMinutes(5);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader("x-api-key");

        if (apiKey != null) {
            try {
                apiKeyRepository.findByCode(Helper.hashKey(apiKey)).ifPresent(key -> {
                    OwnUser user = key.getUser();
                    CustomUserDetail customUserDetail =
                            CustomUserDetail.builder().user(user).build();
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            customUserDetail,
                            null,
                            customUserDetail.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    updateLastUsedIfNeeded(key);
                });
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        filterChain.doFilter(request, response);
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