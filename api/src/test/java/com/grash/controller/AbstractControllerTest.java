package com.grash.controller;

import com.grash.exception.GlobalExceptionHandlerController;
import com.grash.repository.ApiKeyRepository;
import com.grash.security.ClientIpResolver;
import com.grash.service.LicenseService;
import com.grash.service.RateLimiterService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Import(GlobalExceptionHandlerController.class)
@EnableMethodSecurity
public class AbstractControllerTest {
    @MockitoBean
    private ApiKeyRepository apiKeyRepository;
    @MockitoBean
    private LicenseService licenseService;
    @MockitoBean
    protected RateLimiterService rateLimiterService;
    @MockitoBean
    private ClientIpResolver clientIpResolver;
}
