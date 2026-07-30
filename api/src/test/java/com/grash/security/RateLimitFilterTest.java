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
    }

    @Test
    void authenticationNull_skipsCheck() throws Exception {
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).resolveAuthenticatedUserBucket(any());
    }

    @Test
    void authenticationNotAuthenticated_skipsCheck() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).resolveAuthenticatedUserBucket(any());
    }

    @Test
    void principalNotCustomUserDetail_skipsCheck() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("not a CustomUserDetail");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(rateLimiterService.isRateLimitEnabled()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).resolveAuthenticatedUserBucket(any());
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
