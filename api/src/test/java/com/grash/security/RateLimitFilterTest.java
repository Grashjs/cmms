package com.grash.security;

import com.grash.model.User;
import com.grash.service.RateLimiterService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private ClientIpResolver clientIpResolver;
    @Mock
    private Bucket bucket;
    @Mock
    private FilterChain filterChain;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RateLimitFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rateLimitDisabled_skipsCheck() throws Exception {
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).resolveAuthenticatedUserBucket(any());
        verify(rateLimiterService, never()).resolveUnAuthenticatedUserBucket(any());
    }

    @Test
    void authenticationNull_appliesUnauthenticatedRateLimit() throws Exception {
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(null);
        when(clientIpResolver.resolve(request)).thenReturn("10.0.0.1");
        when(rateLimiterService.resolveUnAuthenticatedUserBucket("10.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).resolveAuthenticatedUserBucket(any());
        verify(clientIpResolver).resolve(request);
        verify(rateLimiterService).resolveUnAuthenticatedUserBucket("10.0.0.1");
    }

    @Test
    void authenticationNotAuthenticated_appliesUnauthenticatedRateLimit() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);
        when(clientIpResolver.resolve(request)).thenReturn("11.11.11.11");
        when(rateLimiterService.resolveUnAuthenticatedUserBucket("11.11.11.11")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).resolveAuthenticatedUserBucket(any());
        verify(clientIpResolver).resolve(request);
        verify(rateLimiterService).resolveUnAuthenticatedUserBucket("11.11.11.11");
    }

    @Test
    void principalNotCustomUserDetail_appliesUnauthenticatedRateLimit() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("not a CustomUserDetail");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);
        when(clientIpResolver.resolve(request)).thenReturn("192.168.1.1");
        when(rateLimiterService.resolveUnAuthenticatedUserBucket("192.168.1.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).resolveAuthenticatedUserBucket(any());
        verify(clientIpResolver).resolve(request);
        verify(rateLimiterService).resolveUnAuthenticatedUserBucket("192.168.1.1");
    }

    @Test
    void unauthenticatedRateLimitExceeded_returns429() throws Exception {
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(null);
        when(clientIpResolver.resolve(request)).thenReturn("10.0.0.1");
        when(rateLimiterService.resolveUnAuthenticatedUserBucket("10.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertTrue(stringWriter.toString().contains("Rate limit exceeded"),
                "Response should contain rate limit error message");
    }

    @Test
    void rateLimitAllowed_proceeds() throws Exception {
        User user = new User();
        user.setId(1L);
        CustomUserDetail userDetail = CustomUserDetail.builder().user(user).build();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);
        when(rateLimiterService.resolveAuthenticatedUserBucket("user:1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void rateLimitExceeded_returns429() throws Exception {
        User user = new User();
        user.setId(1L);
        CustomUserDetail userDetail = CustomUserDetail.builder().user(user).build();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);
        when(rateLimiterService.resolveAuthenticatedUserBucket("user:1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertTrue(stringWriter.toString().contains("Rate limit exceeded"),
                "Response should contain rate limit error message");
    }
}
