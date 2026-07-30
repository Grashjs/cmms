package com.grash.security;

import com.grash.model.User;
import com.grash.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserResolverTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private CurrentUserResolver resolver;

    @Test
    void supportsParameter_withCurrentUserAnnotation_returnsTrue() {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.getParameterAnnotation(CurrentUser.class)).thenReturn(mock(CurrentUser.class));

        assertTrue(resolver.supportsParameter(parameter));
    }

    @Test
    void supportsParameter_withoutCurrentUserAnnotation_returnsFalse() {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.getParameterAnnotation(CurrentUser.class)).thenReturn(null);

        assertFalse(resolver.supportsParameter(parameter));
    }

    @Test
    void resolveArgument_returnsUserFromUserService() throws Exception {
        MethodParameter parameter = mock(MethodParameter.class);
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        User expectedUser = new User();
        expectedUser.setEmail("john@test.com");
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(httpRequest);
        when(userService.whoami(httpRequest)).thenReturn(expectedUser);

        User result = resolver.resolveArgument(parameter, null, webRequest, null);

        assertSame(expectedUser, result);
    }
}
