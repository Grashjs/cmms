package com.grash.configuration;

import com.grash.security.OAuth2Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@ConditionalOnProperty(name = "enable-sso", havingValue = "true")
public class OAuth2ClientRegistrationConfig {

    public enum Oauth2Provider {GOOGLE, MICROSOFT}

    @Value("${api.host}")
    private String PUBLIC_API_URL;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OAuth2Properties properties) {
        Oauth2Provider provider = Oauth2Provider.valueOf(properties.getProvider().toUpperCase());

        ClientRegistration registration;
        String clientId = System.getenv("OAUTH2_CLIENT_ID");
        String clientSecret = System.getenv("OAUTH2_CLIENT_SECRET");
        switch (provider) {
            case GOOGLE:
                registration = CommonOAuth2Provider.GOOGLE
                        .getBuilder("google")
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .redirectUri(PUBLIC_API_URL + "/oauth2/callback/{registrationId}")
                        .scope("email", "profile")
                        .build();
                break;
            case MICROSOFT:
                registration = ClientRegistration.withRegistrationId("microsoft")
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .redirectUri(PUBLIC_API_URL + "/oauth2/callback/{registrationId}")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
                        .scope("email", "profile", "openid")
                        .authorizationUri("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                        .tokenUri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                        .jwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys")
                        .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                        .userNameAttributeName("sub")
                        .build();
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return new InMemoryClientRegistrationRepository(registration);
    }
}
