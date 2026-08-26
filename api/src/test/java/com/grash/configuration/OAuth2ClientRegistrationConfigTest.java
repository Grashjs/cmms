package com.grash.configuration;

import com.grash.security.OAuth2Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuth2ClientRegistrationConfigTest {

    private final OAuth2ClientRegistrationConfig config = new OAuth2ClientRegistrationConfig();

    private OAuth2Properties properties;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(config, "PUBLIC_API_URL", "http://localhost:8080");
        properties = new OAuth2Properties();
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
    }

    @Test
    void clientRegistrationRepository_googleProvider_buildsGoogleRegistration() {
        properties.setProvider("google");

        ClientRegistrationRepository repository = config.clientRegistrationRepository(properties);

        ClientRegistration registration = repository.findByRegistrationId("google");
        assertNotNull(registration);
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, registration.getAuthorizationGrantType());
        assertEquals("http://localhost:8080/oauth2/callback/{registrationId}", registration.getRedirectUri());
    }

    @Test
    void clientRegistrationRepository_microsoftProvider_buildsMicrosoftRegistration() {
        properties.setProvider("microsoft");

        ClientRegistrationRepository repository = config.clientRegistrationRepository(properties);

        ClientRegistration registration = repository.findByRegistrationId("microsoft");
        assertNotNull(registration);
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                registration.getProviderDetails().getAuthorizationUri());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, registration.getAuthorizationGrantType());
        assertEquals("http://localhost:8080/oauth2/callback/{registrationId}", registration.getRedirectUri());
    }

    @Test
    void clientRegistrationRepository_unsupportedProvider_throws() {
        properties.setProvider("github");

        assertThrows(IllegalArgumentException.class, () -> config.clientRegistrationRepository(properties));
    }
}
