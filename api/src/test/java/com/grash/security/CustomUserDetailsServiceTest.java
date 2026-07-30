package com.grash.security;

import com.grash.model.User;
import com.grash.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_returnsCustomUserDetailForUser() {
        User user = new User();
        user.setEmail("john@test.com");
        when(userService.whoami("john@test.com", true)).thenReturn(user);

        CustomUserDetail result = customUserDetailsService.loadUserByUsername("john@test.com");

        assertNotNull(result);
        assertSame(user, result.getUser());
        assertEquals("john@test.com", result.getUsername());
    }

    @Test
    void loadUserByUsername_callsWhoamiWithCorrectParameters() {
        User user = new User();
        user.setEmail("test@test.com");
        when(userService.whoami("test@test.com", true)).thenReturn(user);

        customUserDetailsService.loadUserByUsername("test@test.com");

        verify(userService).whoami("test@test.com", true);
    }
}
