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
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final LicenseService licenseService;
    @Value("${enable-sso}")
    private boolean enableSso;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // Disable CSRF (cross site request forgery)
        http.csrf().disable();

        // No session will be created or used by spring security
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // Entry points
        http.authorizeRequests()//
                .antMatchers("/auth/signin").permitAll()//
                .antMatchers("/auth/signup").permitAll()//
                .antMatchers("/auth/sso/**").permitAll()//
                .antMatchers("/auth/sendMail").permitAll()//
                .antMatchers("/auth/resetpwd/**").permitAll()
                .antMatchers("/oauth2/**").permitAll()
                .antMatchers("/login/oauth2/**").permitAll()
                .antMatchers("/fast-spring/**").permitAll()
                .antMatchers("/health-check").permitAll()
                .antMatchers("/mail/send").permitAll()
                .antMatchers("/subscription-plans").permitAll()
                .antMatchers("/files/download/tos", "/files/download/privacy-policy").permitAll()
                .antMatchers("/ws/**").permitAll()
                .antMatchers(HttpMethod.POST, "/newsLetters").permitAll()
                .antMatchers("/auth/activate-account**").permitAll()//
                .antMatchers("/auth/reset-pwd-confirm**").permitAll()//
                .antMatchers("/h2-console/**/**").permitAll()
                // Disallow everything else..
                .anyRequest().authenticated();

        // OAuth2 Configuration
        if (enableSso && licenseService.isSSOEnabled()) http.oauth2Login()
                .authorizationEndpoint()
                .baseUri("/oauth2/authorize")
                .and()
                .redirectionEndpoint()
                .baseUri("/oauth2/callback/*")
                .and()
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler);

        // If a user try to access a resource without having enough permissions
        http.exceptionHandling().accessDeniedPage("/login");

        // Apply JWT
        http.apply(new JwtTokenFilterConfigurer(jwtTokenProvider));
        http.cors();

        // Optional, if you want to test the API from a browser
        // http.httpBasic();
    }

    @Override
    public void configure(WebSecurity web) {
        // Allow swagger to be accessed without authentication
        web.ignoring().antMatchers("/v2/api-docs")//
                .antMatchers("/swagger-resources/**")//
                .antMatchers("/swagger-ui.html")//
                .antMatchers("/com/grash/configuration/**")//
                .antMatchers("/webjars/**")//
                .antMatchers("/public")
                .antMatchers("/images/**")
                // Un-secure H2 Database (for testing purposes, H2 console shouldn't be unprotected in production)
                .and()
                .ignoring()
                .antMatchers("/h2-console/**/**");
        ;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

}
