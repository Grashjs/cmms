package com.grash.configuration;

import io.sentry.Sentry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SentryGlobalErrorFilter {

    @Bean
    public FilterRegistrationBean<SentryGlobalErrorCaptureFilter> sentryGlobalErrorCaptureFilter() {
        FilterRegistrationBean<SentryGlobalErrorCaptureFilter> registration =
                new FilterRegistrationBean<>(new SentryGlobalErrorCaptureFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    static class SentryGlobalErrorCaptureFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception ex) {
                Sentry.captureException(ex);
                throw ex;
            }
        }
    }
}