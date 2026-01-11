package com.grash.configuration;

import com.grash.security.JwtTokenFilterConfigurer;
import com.grash.security.JwtTokenProvider;
import com.grash.security.OAuth2AuthenticationFailureHandler;
import com.grash.security.OAuth2AuthenticationSuccessHandler;
import com.grash.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final LicenseService licenseService;
    @Value("${enable-sso}")
    private boolean enableSso;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Disable CSRF (cross site request forgery)
        http.csrf(csrf -> csrf.disable());

        // No session will be created or used by spring security
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Entry points
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/signin").permitAll()
                .requestMatchers("/auth/signup").permitAll()
                .requestMatchers("/auth/sso/**").permitAll()
                .requestMatchers("/auth/sendMail").permitAll()
                .requestMatchers("/auth/resetpwd/**").permitAll()
                .requestMatchers("/license/state").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/health-check").permitAll()
                .requestMatchers("/mail/send").permitAll()
                .requestMatchers("/subscription-plans").permitAll()
                .requestMatchers("/files/download/tos", "/files/download/privacy-policy").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/newsLetters").permitAll()
                .requestMatchers("/auth/activate-account**").permitAll()
                .requestMatchers("/demo/generate-account").permitAll()
                .requestMatchers("/webhooks/**").permitAll()
                .requestMatchers("/paddle/create-checkout-session").permitAll()
                .requestMatchers("/auth/reset-pwd-confirm**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                // Disallow everything else..
                .anyRequest().authenticated()
        );

        // OAuth2 Configuration
        if (enableSso && licenseService.isSSOEnabled()) {
            http.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(endpoint -> endpoint.baseUri("/oauth2/authorize"))
                    .redirectionEndpoint(endpoint -> endpoint.baseUri("/oauth2/callback/*"))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler)
            );
        }

        // If a user try to access a resource without having enough permissions
        http.exceptionHandling(exception -> exception.accessDeniedPage("/login"));

        // Apply JWT
        http.apply(new JwtTokenFilterConfigurer(jwtTokenProvider));
        http.cors(cors -> {
        }); // Using lambda for cors configuration

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/com/grash/configuration/**")//
                .requestMatchers("/webjars/**")//
                .requestMatchers("/public")
                .requestMatchers("/images/**")
                // Un-secure H2 Database (for testing purposes, H2 console shouldn\'t be unprotected in production)
//                .requestMatchers("/h2-console/**")
                ;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


}
