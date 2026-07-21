package com.grash.integration;

import com.grash.model.File;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
public abstract class MockedServicesTestBase extends AbstractIntegrationTest {

    @MockBean
    protected EmailService2 emailService2;
    @MockBean
    protected SendgridService sendgridService;

    @MockBean
    protected GCPService gcpService;

    @MockBean
    protected MinioService minioService;

    @BeforeEach
    void setUpMocks() {
        when(gcpService.generateSignedUrl(any(File.class), anyLong()))
                .thenReturn(UUID.randomUUID().toString());
        when(minioService.generateSignedUrl(any(File.class), anyLong()))
                .thenReturn(UUID.randomUUID().toString());
    }
}