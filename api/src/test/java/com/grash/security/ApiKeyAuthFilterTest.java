package com.grash.security;

import com.grash.exception.CustomException;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.security.NoSuchAlgorithmException;
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

    @InjectMocks
    private ApiKeyAuthFilter filter;

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
    }

    @Test
    void apiKeyNotFound_noAuthentication_andContinuesChain() throws Exception {
        String rawKey = "unknown-key";
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
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
    void validApiKeyWithoutEntitlement_throwsCustomException() throws Exception {
        String rawKey = "no-entitlement-key";
        User user = createUserWithAccess();
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> filter.doFilterInternal(request, response, filterChain));
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void validApiKeyWithoutPlanFeature_throwsCustomException() throws Exception {
        String rawKey = "no-feature-key";
        User user = createUserWithAccess();
        user.getCompany().getSubscription().getSubscriptionPlan().setFeatures(new HashSet<>());
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));
        when(licenseService.hasEntitlement(any())).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> filter.doFilterInternal(request, response, filterChain));
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void disabledUser_throwsCustomException() throws Exception {
        String rawKey = "disabled-user-key";
        User user = createUserWithAccess();
        user.setEnabled(false);
        ApiKey apiKey = createApiKey(user);
        when(request.getHeader("x-api-key")).thenReturn(rawKey);
        when(apiKeyRepository.findByCode(Helper.hashKey(rawKey))).thenReturn(Optional.of(apiKey));

        CustomException ex = assertThrows(CustomException.class,
                () -> filter.doFilterInternal(request, response, filterChain));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        assertEquals("User account is disabled", ex.getMessage());
    }
}
