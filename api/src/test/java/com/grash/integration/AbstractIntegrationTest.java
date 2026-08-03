package com.grash.integration;

import com.grash.factory.MailServiceFactory;
import com.grash.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public abstract class AbstractIntegrationTest extends AbstractTestContainer {

    @MockBean
    protected MailServiceFactory mailServiceFactory;

    protected MailService mailService;

    @BeforeEach
    void setUpMocks() {
        mailService = mock(MailService.class);
        when(mailServiceFactory.getMailService()).thenReturn(mailService);

    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}