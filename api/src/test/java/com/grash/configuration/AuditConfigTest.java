package com.grash.configuration;

import com.grash.model.User;
import com.grash.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditConfigTest {

    @Mock
    private UserService userService;

    private AuditorAware<Long> auditorProvider;

    @BeforeEach
    void setUp() {
        AuditConfig auditConfig = new AuditConfig(userService);
        auditorProvider = auditConfig.auditorProvider();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_noAuthentication_returnsEmpty() {
        SecurityContextHolder.clearContext();

        assertTrue(auditorProvider.getCurrentAuditor().isEmpty());
    }

    @Test
    void getCurrentAuditor_anonymousToken_returnsEmpty() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertTrue(auditorProvider.getCurrentAuditor().isEmpty());
    }

    @Test
    void getCurrentAuditor_notAuthenticated_returnsEmpty() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("john@test.com", "pw");
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertTrue(auditorProvider.getCurrentAuditor().isEmpty());
    }

    @Test
    void getCurrentAuditor_authenticatedUserFound_returnsUserId() {
        User user = new User();
        user.setId(42L);
        when(userService.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "john@test.com", "pw", AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertEquals(Optional.of(42L), auditorProvider.getCurrentAuditor());
    }

    @Test
    void getCurrentAuditor_authenticatedUserNotFound_returnsEmpty() {
        when(userService.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "nobody@test.com", "pw", AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertTrue(auditorProvider.getCurrentAuditor().isEmpty());
    }
}
