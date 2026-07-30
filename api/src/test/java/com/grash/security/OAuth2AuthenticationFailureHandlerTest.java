package com.grash.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private OAuth2Properties oAuth2Properties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException exception;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler handler;

    @Test
    void onAuthenticationFailure_redirectsToFailureUrlWithErrorParameter() throws Exception {
        when(oAuth2Properties.getFailureRedirectUrl()).thenReturn("https://app.com/oauth-error");
        when(exception.getLocalizedMessage()).thenReturn("Access denied");

        handler.setRedirectStrategy(redirectStrategy);
        handler.onAuthenticationFailure(request, response, exception);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response),
                argThat(url -> ((String) url).equals("https://app.com/oauth-error?error=Access denied")));
    }
}
