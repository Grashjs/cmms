package com.grash.integration;

import com.grash.factory.MailServiceFactory;
import com.grash.model.File;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public abstract class MockedServicesTestBase extends AbstractIntegrationTest {

    @MockBean
    protected MailServiceFactory mailServiceFactory;

    protected MailService mailService;

    @BeforeEach
    void setUpMocks() {
        mailService = mock(MailService.class);
        when(mailServiceFactory.getMailService()).thenReturn(mailService);

    }
}