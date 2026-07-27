package com.grash.controller;

import com.grash.exception.GlobalExceptionHandlerController;
import com.grash.repository.ApiKeyRepository;
import com.grash.service.LicenseService;
import com.grash.service.RateLimiterService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Import(GlobalExceptionHandlerController.class)
@EnableMethodSecurity
public class AbstractControllerTest {
    @MockBean
    private ApiKeyRepository apiKeyRepository;
    @MockBean
    private LicenseService licenseService;
    @MockBean
    private RateLimiterService rateLimiterService;
}
