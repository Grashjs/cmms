package com.grash.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.model.ApiKey;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.User;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.repository.ApiKeyRepository;
import com.grash.service.LicenseService;
import com.grash.utils.Helper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private LicenseService licenseService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApiKeyAuthFilter filter;

    private ByteArrayOutputStream responseOutputStream;

    @BeforeEach
    void setUp() throws Exception {
        responseOutputStream = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener listener) {
            }

            @Override
            public void write(int b) {
                responseOutputStream.write(b);
            }
        };
        lenient().when(response.getOutputStream()).thenReturn(servletOutputStream);
    }

    private User createUserWithAccess() {
        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .features(new HashSet<>(Set.of(PlanFeatures.API_ACCESS)))
                .build();
        Subscription subscription = Subscription.builder()
                .subscriptionPlan(plan)
                .build();
        Company company = new Company("TestCo", 10, subscription);

        User user = new User();
        user.setId(1L);
        user.setEmail("api@test.com");
        user.setRole(role);
        user.setEnabled(true);
        user.setCompany(company);
        return user;
    }

    private ApiKey createApiKey(User user) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(1L);
        apiKey.setLabel("Test Key");
        apiKey.setCode("hashed-key");
        apiKey.setUser(user);
        return apiKey;
    }

    @Test
    void noApiKeyHeader_noAuthentication_andContinuesChain() throws Exception {
        when(request.getHeader("x-api-key")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void apiKeyNotFound_noAuthentication_andContinuesChain() throws Exception {
        String rawKey = "unknown-key";
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void validApiKeyWithAccess_setsAuthentication_andContinuesChain() throws Exception {
        String rawKey = "valid-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(apiKeyRepository).updateLastUsed(eq(1L), any());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void validApiKeyWithAccess_authenticationContextSet() throws Exception {
        String rawKey = "valid-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void validApiKeyWithoutEntitlement_returns403_andStopsChain() throws Exception {
        String rawKey = "no-entitlement-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void validApiKeyWithoutPlanFeature_returns403_andStopsChain() throws Exception {
        String rawKey = "no-feature-key";
        User user = createUserWithAccess();
        user.getCompany().getSubscription().getSubscriptionPlan().setFeatures(new HashSet<>());
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void disabledUser_returns401_andStopsChain() throws Exception {
        String rawKey = "disabled-user-key";
        User user = createUserWithAccess();
        user.setEnabled(false);
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void revokedApiKey_returns401_andStopsChain() throws Exception {
        String rawKey = "revoked-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        apiKey.setRevokedAt(new Date());
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void expiredApiKey_returns401_andStopsChain() throws Exception {
        String rawKey = "expired-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        apiKey.setExpiresAt(new Date(System.currentTimeMillis() - 100000));
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void futureExpirationApiKey_authenticatesSuccessfully() throws Exception {
        String rawKey = "future-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        apiKey.setExpiresAt(new Date(System.currentTimeMillis() + 100000));
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void nullExpirationApiKey_authenticatesSuccessfully() throws Exception {
        String rawKey = "no-expiry-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        apiKey.setExpiresAt(null);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
