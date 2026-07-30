package com.grash.security;

import com.grash.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtTokenFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nullToken_doesNotSetAuthentication_andContinuesChain() throws Exception {
        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_setsAuthentication_andContinuesChain() throws Exception {
        String token = "valid-jwt";
        Authentication auth = mock(Authentication.class);
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_clearsContext_andSendsError() throws Exception {
        String token = "invalid-jwt";
        CustomException ex = new CustomException("Expired or invalid JWT token", HttpStatus.INTERNAL_SERVER_ERROR);
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenThrow(ex);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Expired or invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validateTokenReturnsFalse_doesNotCallGetAuthentication_andContinuesChain() throws Exception {
        String token = "invalid-but-not-throwing";
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void getAuthenticationThrows_clearsContext_andSendsError() throws Exception {
        String token = "token-that-passes-validation";
        CustomException ex = new CustomException("User account is disabled", HttpStatus.UNAUTHORIZED);
        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenThrow(ex);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response).sendError(HttpStatus.UNAUTHORIZED.value(), "User account is disabled");
        verify(filterChain, never()).doFilter(request, response);
    }
}
