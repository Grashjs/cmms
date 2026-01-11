package com.grash.configuration;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.model.OwnUser;
import com.grash.model.enums.PlanFeatures;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenFilter;
import com.grash.security.JwtTokenProvider;
import com.grash.service.LicenseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SwaggerSecurityConfig {

    @Bean
    SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http, LicenseService licenseService,
                                                   JwtTokenProvider jwtTokenProvider) throws Exception {
        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
                .addFilterBefore(new JwtTokenFilter(jwtTokenProvider),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().access((authentication, context) -> {
                            if (authentication.get() == null) {
                                return new AuthorizationDecision(false);
                            }

                            OwnUser user = ((CustomUserDetail) authentication.get().getPrincipal()).getUser();

                            boolean allowed =
                                    user.getCompany()
                                            .getSubscription()
                                            .getSubscriptionPlan()
                                            .getFeatures()
                                            .contains(PlanFeatures.API_ACCESS) &&
                                            licenseService.hasEntitlement(LicenseEntitlement.API_ACCESS);

                            return new AuthorizationDecision(allowed);
                        })
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
