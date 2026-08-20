package com.grash.configuration;

import com.grash.security.ApiKeyAuthFilter;
import com.grash.security.CustomUserDetail;
import com.grash.security.CustomUserDetailsService;
import com.grash.security.JwtTokenProvider;
import com.grash.security.OAuth2AuthenticationFailureHandler;
import com.grash.security.OAuth2AuthenticationSuccessHandler;
import com.grash.security.RateLimitFilter;
import com.grash.service.LicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSecurityConfigTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @Mock
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @Mock
    private LicenseService licenseService;
    @Mock
    private RateLimitFilter rateLimitFilter;
    @Mock
    private CustomUserDetailsService customUserDetailsService;

    private WebSecurityConfig webSecurityConfig;

    @BeforeEach
    void setUp() {
        webSecurityConfig = new WebSecurityConfig(
                jwtTokenProvider,
                oAuth2AuthenticationSuccessHandler,
                oAuth2AuthenticationFailureHandler,
                licenseService,
                rateLimitFilter,
                customUserDetailsService,
                Optional.empty(),
                corsConfigurationSource());
        ReflectionTestUtils.setField(webSecurityConfig, "enableSso", false);
        ReflectionTestUtils.setField(webSecurityConfig, "ldapEnabled", false);
    }

    private CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration());
        return source;
    }

    @Test
    void passwordEncoder_encodesAndMatches() {
        PasswordEncoder encoder = webSecurityConfig.passwordEncoder();

        String encoded = encoder.encode("secret");
        assertTrue(encoder.matches("secret", encoded));
        assertTrue(!encoder.matches("wrong", encoded));
    }

    @Test
    void authenticationManager_whenLdapDisabled_hasSingleProvider() {
        AuthenticationManager authenticationManager = webSecurityConfig.authenticationManager();

        ProviderManager providerManager = assertInstanceOf(ProviderManager.class, authenticationManager);
        assertTrue(providerManager.getProviders().stream()
                .allMatch(provider -> provider instanceof DaoAuthenticationProvider));
    }

    @Test
    void authenticationManager_whenLdapEnabledIncludesLdapProvider() {
        webSecurityConfig = new WebSecurityConfig(
                jwtTokenProvider,
                oAuth2AuthenticationSuccessHandler,
                oAuth2AuthenticationFailureHandler,
                licenseService,
                rateLimitFilter,
                customUserDetailsService,
                Optional.of(mock(LdapAuthenticationProvider.class)),
                corsConfigurationSource());
        ReflectionTestUtils.setField(webSecurityConfig, "ldapEnabled", true);

        AuthenticationManager authenticationManager = webSecurityConfig.authenticationManager();

        ProviderManager providerManager = assertInstanceOf(ProviderManager.class, authenticationManager);
        assertTrue(providerManager.getProviders().stream()
                .anyMatch(provider -> provider instanceof LdapAuthenticationProvider));
    }

    @Test
    void authenticationManager_authenticatesValidCredentials() {
        PasswordEncoder encoder = webSecurityConfig.passwordEncoder();
        CustomUserDetail details = mock(CustomUserDetail.class);
        when(details.getPassword()).thenReturn(encoder.encode("secret"));
        when(details.isEnabled()).thenReturn(true);
        when(details.isAccountNonExpired()).thenReturn(true);
        when(details.isAccountNonLocked()).thenReturn(true);
        when(details.isCredentialsNonExpired()).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("john@test.com")).thenReturn(details);

        Authentication authentication = webSecurityConfig.authenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken("john@test.com", "secret"));

        assertTrue(authentication.isAuthenticated());
    }

    @Test
    void filterChain_buildsSecurityFilterChain() throws Exception {
        ApiKeyAuthFilter apiKeyAuthFilter = mock(ApiKeyAuthFilter.class);

        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        AuthenticationManagerBuilder authBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(AuthenticationManagerBuilder.class, authBuilder);
        sharedObjects.put(ApplicationContext.class, realWebContext());
        HttpSecurity http = new HttpSecurity(objectPostProcessor, authBuilder, sharedObjects);

        SecurityFilterChain chain = webSecurityConfig.filterChain(http, apiKeyAuthFilter);

        assertNotNull(chain);
    }

    private static ApplicationContext realWebContext() {
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.registerBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class);
        context.refresh();
        return context;
    }
}
