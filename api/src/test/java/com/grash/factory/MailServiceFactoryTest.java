package com.grash.factory;

import com.grash.model.enums.MailType;
import com.grash.service.EmailService2;
import com.grash.service.MailService;
import com.grash.service.SendgridService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class MailServiceFactoryTest {

    @Mock
    private EmailService2 emailService2;

    @Mock
    private SendgridService sendgridService;

    @InjectMocks
    private MailServiceFactory mailServiceFactory;

    private void setMailType(MailType mailType) {
        ReflectionTestUtils.setField(mailServiceFactory, "mailType", mailType);
    }

    @Test
    void getMailService_sendgrid_returnsSendgridService() {
        setMailType(MailType.SENDGRID);

        MailService result = mailServiceFactory.getMailService();

        assertSame(sendgridService, result);
    }

    @Test
    void getMailService_smtp_returnsEmailService2() {
        setMailType(MailType.SMTP);

        MailService result = mailServiceFactory.getMailService();

        assertSame(emailService2, result);
    }
}
